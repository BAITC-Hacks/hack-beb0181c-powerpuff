package com.powerpuff.backend;

import com.powerpuff.backend.entity.Product;
import com.powerpuff.backend.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import com.powerpuff.backend.client.EktClient;
import com.powerpuff.backend.client.EktDtos;
import com.powerpuff.backend.service.sync.CatalogSynchronizer;
import com.powerpuff.backend.service.sync.CatalogSyncStore;
import com.powerpuff.backend.exception.EktException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Testcontainers
@EnabledIfSystemProperty(named = "postgresTests", matches = "true")
class PostgresMigrationTest {
    @Container static final PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
        r.add("app.ekt.enabled", () -> false);
    }
    @Autowired org.springframework.test.web.servlet.MockMvc http;
    @Autowired com.powerpuff.backend.service.CatalogSearchService search;
    @Test void searchRanksExactArticlesAndEscapesWildcards() {
        jdbc.update("INSERT INTO products(id,name,article,supplier_article,brand,price) VALUES (1,'Generic','Ab_01',NULL,'Brand',0.01),(2,'Generic','other','Ab_01','Brand',NULL),(3,'Contains Ab_01','ABX01',NULL,'Brand',NULL),(4,'Contains AbX01','four',NULL,'Brand',NULL)");
        var result=search.search("  aB_01  ",0,20);
        assertThat(result.items()).extracting(i->i.id()).containsExactly(1L,2L,3L);
        assertThat(result.items().get(1).quantity()).isNull();
        assertThat(search.search("Ab%01",0,20).items()).isEmpty();
        assertThat(search.search("' OR 1=1 --",0,20).items()).isEmpty();
        assertThat(search.search("missing",0,20).totalElements()).isZero();
        verifyNoInteractions(ekt);
    }
    @Test void searchSupportsCyrillicBrandAndStablePages() {
        jdbc.update("INSERT INTO products(id,name,article,brand,list_updated_at,detail_updated_at) VALUES (1,'Автомат 160А','001_','Legrand',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(2,'Автомат 160А','002_','Legrand',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(3,'Лампа','003_','Other',NULL,NULL)");
        var first=search.search("АВТОМАТ legrand",0,1);
        assertThat(first.totalElements()).isEqualTo(2);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.items()).extracting(i->i.id()).containsExactly(1L);
        assertThat(first.items().getFirst().listUpdatedAt()).isNotNull();
        assertThat(search.search("автомат LEGRAND",1,1).items()).extracting(i->i.id()).containsExactly(2L);
        assertThat(search.search("автомат LEGRAND",2,1).items()).isEmpty();
        verifyNoInteractions(ekt);
    }
    @Autowired com.powerpuff.backend.commerce.SessionService sessions;
    @Autowired com.powerpuff.backend.commerce.CartService carts;
    @Autowired com.powerpuff.backend.chat.ChatService chat;
    @Autowired com.powerpuff.backend.repository.StoredCatalogRepository stored;
    @MockitoBean com.powerpuff.backend.chat.LlmClient llm;
    java.util.UUID customer() {return sessions.authenticate("Bearer "+sessions.create().get("token"));}
    @Test void cartNeedsConfirmationAndRejectsOtherSession() throws Exception {
        var owner=customer();var stranger=customer();
        when(ekt.getProduct(1)).thenReturn(item(1));
        var proposal=carts.create(owner,java.util.UUID.randomUUID(),1,new BigDecimal("0.1"));
        assertThat((List<?>)carts.cart(owner).get("items")).isEmpty();
        assertThatThrownBy(()->carts.confirm(stranger,proposal.id(),1)).isInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
        assertThat(carts.confirm(owner,proposal.id(),1).added()).isTrue();
        carts.confirm(owner,proposal.id(),1);
        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_items WHERE session_id=?",BigDecimal.class,owner)).isEqualByComparingTo("0.1");
        assertThatThrownBy(()->carts.create(owner,java.util.UUID.randomUUID(),1,new BigDecimal("0.1"))).isInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
    }
    @Test void priceChangeRequiresAnotherConfirmation() throws Exception {
        var owner=customer();var initial=item(1);
        when(ekt.getProduct(1)).thenReturn(initial);
        var p=carts.create(owner,java.util.UUID.randomUUID(),1,new BigDecimal("0.1"));
        var changed=new EktDtos.Product(1L,initial.name(),initial.article(),initial.description(),new BigDecimal("2"),initial.quantity(),initial.stores(),null,null,initial.offers(),initial.properties());
        when(ekt.getProduct(1)).thenReturn(changed);
        var c=carts.confirm(owner,p.id(),1);
        assertThat(c.requiresConfirmation()).isTrue();
        assertThat((List<?>)c.cart().get("items")).isEmpty();
        assertThatThrownBy(()->carts.confirm(owner,p.id(),1)).isInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
        assertThat(carts.confirm(owner,p.id(),2).added()).isTrue();
    }
    @Test void concurrentConfirmationsDoNotDuplicate() throws Exception {
        var owner=customer();when(ekt.getProduct(1)).thenReturn(item(1));
        var p=carts.create(owner,java.util.UUID.randomUUID(),1,new BigDecimal("0.1"));
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->carts.confirm(owner,p.id(),1));var b=pool.submit(()->carts.confirm(owner,p.id(),1));
            assertThat(a.get().added()).isTrue();assertThat(b.get().added()).isTrue();
        }
        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_items WHERE session_id=?",BigDecimal.class,owner)).isEqualByComparingTo("0.1");
    }
    @Test void chatProposesThenConfirmsAndCachesMessage() throws Exception {
        var owner=customer();when(ekt.getProduct(1)).thenReturn(item(1));
        when(llm.route(anyString(),anyList())).thenReturn(new com.powerpuff.backend.chat.LlmClient.Action("propose",null,1L,"0.1"));
        var message=java.util.UUID.randomUUID();
        var p=chat.send(owner,message,"Добавь 0.1 товара 1");
        assertThat(chat.send(owner,message,"Добавь 0.1 товара 1")).isEqualTo(mapper.readTree(p.toString()));
        assertThat((List<?>)carts.cart(owner).get("items")).isEmpty();
        assertThat(chat.send(owner,java.util.UUID.randomUUID(),"да, добавь").path("data").path("added").asBoolean()).isTrue();
        verify(llm,times(1)).route(anyString(),anyList());
    }
    @Test void storedCardFlagsConflictingCurrent() throws Exception {
        when(ekt.getProducts(1)).thenReturn(page(1,item(1)));when(ekt.getProduct(1)).thenReturn(item(1));
        sync.sync(false,1,1,0);
        assertThat(stored.get(1).warnings()).contains("NOMINAL_CURRENT_CONFLICT");
        assertThat(stored.get(1).product().stores()).hasSize(1);
    }
    @Autowired com.powerpuff.backend.service.AnalogService analogService;
    @Test void analogsRequireFullMatchingParameters() throws Exception {
        String properties="{\"OBYEM\":\"Автоматический выключатель\",\"KOLICHESTVO_POLYUSOV\":\"3\",\"NOMINALNOE_NAPRYAZHENIE\":\"400В\",\"NOMINALNYY_TOK\":\"160 А\",\"NOMINALNAYA_OTKLYUCHAYUSHCHAYA_SPOSOBNOST\":\"18кА\",\"TIP_USTANOVKI\":\"Винтовое\",\"KHARAKTERISTIKA_SRABATYVANIYA\":\"C\"}";
        jdbc.update("INSERT INTO products(id,name,quantity,properties,detail_updated_at) VALUES (1,'160A source',0,CAST(? AS JSONB),CURRENT_TIMESTAMP),(2,'160A candidate',2,CAST(? AS JSONB),CURRENT_TIMESTAMP)",properties,properties);
        assertThat((List<?>)analogService.find(1).get("items")).hasSize(1);
        assertThat(analogService.find(1).get("confirmed")).isEqualTo(false);
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/products/1/analogs"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.confirmed").value(false))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].compatibilityStatus").value("UNCONFIRMED_CANDIDATE"));
        assertThat(analogService.find(1).get("message").toString()).contains("Товар отсутствует", "Подтверждённых аналогов не найдено", "менеджера");
        jdbc.update("UPDATE products SET name='160А candidate',description='Номинальный ток 250 А' WHERE id=2");
        assertThat((List<?>)analogService.find(1).get("items")).isEmpty();
        jdbc.update("UPDATE products SET description=NULL WHERE id=2");
        jdbc.update("UPDATE products SET properties=jsonb_set(properties,'{NOMINALNYY_TOK}','\"250 А\"'::jsonb) WHERE id=2");
        assertThat((List<?>)analogService.find(1).get("items")).isEmpty();
    }
    @Test void chatWarnsAboutConflictAndExplainsNoAnalogsInMainText() throws Exception {
        var owner=customer();
        when(ekt.getProduct(1)).thenReturn(item(1));
        when(llm.route(anyString(),anyList())).thenReturn(new com.powerpuff.backend.chat.LlmClient.Action("product",null,1L,null));
        var card=chat.send(owner,java.util.UUID.randomUUID(),"Покажи товар 1");
        assertThat(card.path("text").asText()).contains("Расхождение", "160", "250");
        jdbc.update("INSERT INTO products(id,name,quantity,detail_updated_at) VALUES (1,'160А source',0,CURRENT_TIMESTAMP)");
        when(llm.route(anyString(),anyList())).thenReturn(new com.powerpuff.backend.chat.LlmClient.Action("analogs",null,1L,null));
        var analog=chat.send(owner,java.util.UUID.randomUUID(),"Подбери аналог");
        assertThat(analog.path("text").asText()).contains("Товар отсутствует", "Подтверждённых аналогов не найдено", "менеджера").doesNotContain("передан");
    }
    @Test void sessionsExpireAndProposalsExpire() throws Exception {
        var owner=customer();when(ekt.getProduct(1)).thenReturn(item(1));
        var p=carts.create(owner,java.util.UUID.randomUUID(),1,new BigDecimal("0.1"));
        jdbc.update("UPDATE cart_proposals SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE id=?",p.id());
        assertThatThrownBy(()->carts.confirm(owner,p.id(),1)).isInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
        var token=sessions.create().get("token");var id=sessions.authenticate("Bearer "+token);
        jdbc.update("UPDATE customer_sessions SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE id=?",id);
        assertThatThrownBy(()->sessions.authenticate("Bearer "+token)).isInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
    }
    EktDtos.Product cartProduct(String price, String stock, String multiple) throws Exception {
        return new EktDtos.Product(700L,"Тестовый автомат","000700_","Тестовый fixture",
                new BigDecimal(price),new BigDecimal(stock),List.of(),"https://ekt.kz/test.jpg",null,
                mapper.readTree("[]"),mapper.readTree("{\"KRATNOST_MIN\":\""+multiple+"\"}"));
    }
    java.util.UUID cartWithTwo() throws Exception {
        var owner=customer(); when(ekt.getProduct(700)).thenReturn(cartProduct("10","20","1"));
        var p=carts.create(owner,java.util.UUID.randomUUID(),700,new BigDecimal("2"));
        carts.confirm(owner,p.id(),1); return owner;
    }
    long cartVersion(java.util.UUID owner) { return ((Number)carts.cart(owner).get("version")).longValue(); }
    BigDecimal cartQuantity(java.util.UUID owner) {
        return jdbc.queryForObject("SELECT quantity FROM cart_items WHERE session_id=? AND product_id=700",BigDecimal.class,owner);
    }
    @Test void targetQuantityIsAbsoluteIdempotentAndPersists() throws Exception {
        var owner=cartWithTwo(); var request=java.util.UUID.randomUUID();long version=cartVersion(owner);
        var result=carts.setQuantity(owner,request,700,new BigDecimal("3"),version);
        assertThat(result.updated()).isTrue(); assertThat(cartQuantity(owner)).isEqualByComparingTo("3");
        assertThat((BigDecimal)result.cart().get("total")).isEqualByComparingTo("30");
        var item=(java.util.Map<?,?>)((List<?>)result.cart().get("items")).getFirst();
        assertThat(item.get("article")).isEqualTo("000700_"); assertThat(item.get("image")).isEqualTo("https://ekt.kz/test.jpg");
        carts.setQuantity(owner,request,700,new BigDecimal("3"),version);
        assertThat(cartQuantity(owner)).isEqualByComparingTo("3");
        carts.setQuantity(owner,java.util.UUID.randomUUID(),700,new BigDecimal("4"),cartVersion(owner));
        carts.setQuantity(owner,request,700,new BigDecimal("3"),version);
        assertThat(cartQuantity(owner)).isEqualByComparingTo("4");
        assertThatThrownBy(()->carts.setQuantity(owner,request,700,new BigDecimal("4"),version))
                .isInstanceOfSatisfying(com.powerpuff.backend.commerce.BusinessException.class,e->assertThat(e.code).isEqualTo("REQUEST_ID_REUSED"));
    }
    @Test void decreaseAndDeleteWorkOfflineAndZeroDoesNotDelete() throws Exception {
        var owner=cartWithTwo(); clearInvocations(ekt);
        when(ekt.getProduct(anyLong())).thenThrow(new EktException(HttpStatus.BAD_GATEWAY,"EKT_UNAVAILABLE","offline"));
        assertThatThrownBy(()->carts.setQuantity(owner,java.util.UUID.randomUUID(),700,BigDecimal.ZERO,cartVersion(owner)))
                .isInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
        carts.setQuantity(owner,java.util.UUID.randomUUID(),700,BigDecimal.ONE,cartVersion(owner));
        assertThat(cartQuantity(owner)).isEqualByComparingTo("1");
        var id=java.util.UUID.randomUUID();long version=cartVersion(owner);
        assertThat((List<?>)carts.delete(owner,id,700,version).get("items")).isEmpty();
        assertThat((BigDecimal)carts.delete(owner,id,700,version).get("total")).isZero();
        verify(ekt,never()).getProduct(anyLong());
    }
    @Test void increaseValidatesStockMultipleAndRequiresPriceConfirmation() throws Exception {
        var owner=cartWithTwo();long version=cartVersion(owner);
        when(ekt.getProduct(700)).thenReturn(cartProduct("10","4","2"));
        assertThatThrownBy(()->carts.setQuantity(owner,java.util.UUID.randomUUID(),700,new BigDecimal("6"),version))
                .isInstanceOfSatisfying(com.powerpuff.backend.commerce.BusinessException.class,e->assertThat(e.code).isEqualTo("INSUFFICIENT_STOCK"));
        assertThatThrownBy(()->carts.setQuantity(owner,java.util.UUID.randomUUID(),700,new BigDecimal("3"),version))
                .isInstanceOfSatisfying(com.powerpuff.backend.commerce.BusinessException.class,e->assertThat(e.code).isEqualTo("INVALID_MULTIPLE"));
        when(ekt.getProduct(700)).thenReturn(cartProduct("12","20","1"));
        var request=java.util.UUID.randomUUID();
        var result=carts.setQuantity(owner,request,700,new BigDecimal("3"),version);
        assertThat(result.requiresConfirmation()).isTrue();assertThat(cartQuantity(owner)).isEqualByComparingTo("2");
        assertThat(carts.setQuantity(owner,request,700,new BigDecimal("3"),version).proposal().id()).isEqualTo(result.proposal().id());
        when(ekt.getProduct(700)).thenReturn(cartProduct("13","20","1"));
        var revised=carts.confirm(owner,result.proposal().id(),1);
        assertThat(revised.requiresConfirmation()).isTrue();assertThat(cartQuantity(owner)).isEqualByComparingTo("2");
        carts.confirm(owner,revised.proposal().id(),2);
        assertThat(cartQuantity(owner)).isEqualByComparingTo("3");
        assertThat((BigDecimal)carts.cart(owner).get("total")).isEqualByComparingTo("39");
    }
    @Test void oldProposalsCannotRestoreDeletedItemOrOverwriteEditedQuantity() throws Exception {
        var owner=cartWithTwo();
        var add=carts.create(owner,java.util.UUID.randomUUID(),700,BigDecimal.ONE);
        carts.delete(owner,java.util.UUID.randomUUID(),700,cartVersion(owner));
        assertThatThrownBy(()->carts.confirm(owner,add.id(),1))
                .isInstanceOfSatisfying(com.powerpuff.backend.commerce.BusinessException.class,e->assertThat(e.code).isEqualTo("PROPOSAL_STALE"));
        assertThat((List<?>)carts.cart(owner).get("items")).isEmpty();
        final var second=cartWithTwo();
        when(ekt.getProduct(700)).thenReturn(cartProduct("12","20","1"));
        var edit=carts.setQuantity(second,java.util.UUID.randomUUID(),700,new BigDecimal("3"),cartVersion(second));
        carts.setQuantity(second,java.util.UUID.randomUUID(),700,BigDecimal.ONE,cartVersion(second));
        assertThatThrownBy(()->carts.confirm(second,edit.proposal().id(),1))
                .isInstanceOfSatisfying(com.powerpuff.backend.commerce.BusinessException.class,e->assertThat(e.code).isEqualTo("PROPOSAL_STALE"));
        assertThat(cartQuantity(second)).isEqualByComparingTo("1");
    }
    @Test void simultaneousTargetsUseVersionAndRepeatedRequestDoesNotAccumulate() throws Exception {
        var owner=cartWithTwo();long version=cartVersion(owner);var request=java.util.UUID.randomUUID();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->carts.setQuantity(owner,request,700,new BigDecimal("3"),version));
            var b=pool.submit(()->carts.setQuantity(owner,request,700,new BigDecimal("3"),version));
            assertThat(a.get().updated()).isTrue();assertThat(b.get().updated()).isTrue();
        }
        assertThat(cartQuantity(owner)).isEqualByComparingTo("3");
        assertThatThrownBy(()->carts.setQuantity(owner,java.util.UUID.randomUUID(),700,new BigDecimal("4"),version))
                .isInstanceOfSatisfying(com.powerpuff.backend.commerce.BusinessException.class,e->assertThat(e.code).isEqualTo("CART_CHANGED"));
    }
    @Test void deletionWinsAgainstAnIncreaseAlreadyFetchingEkt() throws Exception {
        var owner=cartWithTwo();long version=cartVersion(owner);var fresh=cartProduct("10","20","1");
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        when(ekt.getProduct(700)).thenAnswer(call->{ entered.countDown();release.await(5,java.util.concurrent.TimeUnit.SECONDS);return fresh; });
        try(var pool=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var update=pool.submit(()->carts.setQuantity(owner,java.util.UUID.randomUUID(),700,new BigDecimal("3"),version));
            assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            carts.delete(owner,java.util.UUID.randomUUID(),700,version);release.countDown();
            assertThatThrownBy(update::get).hasCauseInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
        } finally {release.countDown();}
        assertThat((List<?>)carts.cart(owner).get("items")).isEmpty();
    }
    @Test void lateAddRequestCannotCreateProposalAgainstDeletedCart() throws Exception {
        var owner=cartWithTwo();long version=cartVersion(owner);var fresh=cartProduct("10","20","1");
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        when(ekt.getProduct(700)).thenAnswer(call->{ entered.countDown();release.await(5,java.util.concurrent.TimeUnit.SECONDS);return fresh; });
        try(var pool=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var proposal=pool.submit(()->carts.create(owner,java.util.UUID.randomUUID(),700,BigDecimal.ONE));
            assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            carts.delete(owner,java.util.UUID.randomUUID(),700,version);release.countDown();
            assertThatThrownBy(proposal::get).hasCauseInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
        } finally { release.countDown(); }
        assertThat((List<?>)carts.cart(owner).get("items")).isEmpty();
    }
    @Test void httpEditsRequireOwnerSessionAndReturnCurrentCart() throws Exception {
        var owner=cartWithTwo();var token=sessions.create().get("token").toString();
        var body=mapper.writeValueAsString(java.util.Map.of("requestId",java.util.UUID.randomUUID(),"quantity",3,"cartVersion",0));
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/cart/items/700")
            .contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/cart/items/700")
            .header("Authorization","Bearer "+token).contentType("application/json").content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/cart/items/700")
            .header("Authorization","Bearer "+token).param("requestId",java.util.UUID.randomUUID().toString()).param("cartVersion","0"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        assertThat(cartQuantity(owner)).isEqualByComparingTo("2");
    }
    @Autowired ProductRepository products;
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogSynchronizer sync;
    @Autowired CatalogSyncStore syncStore;
    @Autowired ObjectMapper mapper;
    @MockitoBean EktClient ekt;
    @BeforeEach void clean() {
        org.springframework.test.util.ReflectionTestUtils.setField(sync,"acceptShortPageWrap",false);
        jdbc.execute("TRUNCATE products, stores, catalog_sync_runs RESTART IDENTITY CASCADE");
    }
    EktDtos.Product item(long id) throws Exception {
        return new EktDtos.Product(id,"160A example","000_", "Example description",
            new BigDecimal("0.01"),new BigDecimal("0.125"),
            List.of(new EktDtos.Store(2L,"Warehouse",new BigDecimal("0.125"))),
            null,null,mapper.readTree("[]"),mapper.readTree("{\"CML2_BAR_CODE\":\"00012\",\"RECOMMEND\":[\"01\"],\"NOMINALNYY_TOK\":\"250 А\"}"));
    }
    EktDtos.Page page(int n,EktDtos.Product... items) { return new EktDtos.Page(n,20,items.length,List.of(items)); }

    @Test void importIsRepeatableAndPreservesJson() throws Exception {
        var p=item(1);
        when(ekt.getProducts(1)).thenReturn(page(1,p));
        doReturn(page(2)).when(ekt).getProducts(2);
        when(ekt.getProduct(1)).thenReturn(p);
        long first=sync.sync(false,10,10,0);
        assertThat(syncStore.load(first).listComplete()).isTrue();
        assertThat(syncStore.remaining(first)).isZero();
        sync.sync(false,10,10,0);
        assertThat(products.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT properties->'RECOMMEND'->>0 FROM products",String.class)).isEqualTo("01");
        assertThat(jdbc.queryForObject("SELECT quantity FROM product_stocks",BigDecimal.class)).isEqualByComparingTo("0.125");
        // Краткая карточка не стирает детали.
        sync.sync(false,1,0,0);
        assertThat(jdbc.queryForObject("SELECT description FROM products",String.class)).isEqualTo("Example description");
        assertThat(jdbc.queryForObject("SELECT barcode FROM products",String.class)).isEqualTo("00012");
    }
    @Test void failedDetailDoesNotEraseExistingStockAndMissingStockIsUnknown() throws Exception {
        var original=item(1);
        when(ekt.getProducts(1)).thenReturn(page(1,original));
        doReturn(page(2)).when(ekt).getProducts(2);
        when(ekt.getProduct(1)).thenReturn(original);
        sync.sync(false,10,10,0);
        when(ekt.getProduct(1)).thenThrow(new EktException(HttpStatus.BAD_GATEWAY,"EKT_INVALID_RESPONSE","safe"));
        sync.sync(false,10,10,0);
        assertThat(jdbc.queryForObject("SELECT quantity FROM products WHERE id=1",BigDecimal.class)).isEqualByComparingTo("0.125");
        assertThat(stored.get(1).warnings()).contains("DETAIL_REFRESH_FAILED");
        doReturn(new EktDtos.Product(1L,original.name(),original.article(),original.description(),original.price(),null,null,null,null,original.offers(),original.properties())).when(ekt).getProduct(1);
        sync.sync(true,0,10,0);
        assertThat(jdbc.queryForObject("SELECT quantity FROM products WHERE id=1",BigDecimal.class)).isNull();
        assertThat(stored.get(1).product().stockStatus()).isEqualTo("UNKNOWN");
    }
    @Test void resumeAfterPageFailure() throws Exception {
        var p=item(1);
        when(ekt.getProducts(1)).thenReturn(page(1,p));
        when(ekt.getProducts(2)).thenThrow(new EktException(HttpStatus.BAD_GATEWAY,"EKT_UNAVAILABLE","safe"));
        assertThatThrownBy(()->sync.sync(false,10,10,0)).isInstanceOf(IllegalStateException.class);
        assertThat(syncStore.load(1).nextPage()).isEqualTo(2);
        assertThat(syncStore.load(1).listComplete()).isFalse();
        doReturn(page(2)).when(ekt).getProducts(2);
        when(ekt.getProduct(1)).thenReturn(p);
        assertThat(sync.sync(true,10,10,0)).isEqualTo(1);
        verify(ekt,times(1)).getProducts(1);
        assertThat(syncStore.remaining(1)).isZero();
    }
    @Test void verifiedShortLastPageWrapCanCompleteOnlyWhenExplicitlyEnabled() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(sync,"acceptShortPageWrap",true);
        when(ekt.getProducts(1)).thenReturn(page(1,item(1)));
        when(ekt.getProducts(2)).thenReturn(page(2,item(2)));
        when(ekt.getProducts(3)).thenReturn(page(3,item(1)));
        when(ekt.getProduct(1)).thenReturn(item(1));
        when(ekt.getProduct(2)).thenReturn(item(2));
        long run=sync.sync(false,10,10,0);
        assertThat(syncStore.load(run).listComplete()).isTrue();
        assertThat(syncStore.remaining(run)).isZero();
        assertThat(jdbc.queryForObject("SELECT end_condition FROM catalog_sync_runs WHERE id=?",String.class,run)).isEqualTo("WRAP_AFTER_VERIFIED_SHORT_PAGE");
        assertThat(products.count()).isEqualTo(2);
    }
    @Test void wrappingAfterAFullPageStillStopsEvenWithOptIn() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(sync,"acceptShortPageWrap",true);
        when(ekt.getProducts(1)).thenReturn(new EktDtos.Page(1,1,1,List.of(item(1))));
        when(ekt.getProducts(2)).thenReturn(new EktDtos.Page(2,1,1,List.of(item(2))));
        when(ekt.getProducts(3)).thenReturn(new EktDtos.Page(3,1,1,List.of(item(1))));
        long run=sync.sync(false,10,10,0);
        assertThat(syncStore.load(run).listComplete()).isFalse();
        assertThat(jdbc.queryForObject("SELECT reason FROM catalog_sync_runs WHERE id=?",String.class,run)).isEqualTo("REPEATED_PAGE");
        verify(ekt,never()).getProduct(anyLong());
    }
    @Test void repeatedPageStopsWithoutClaimingCompletion() throws Exception {
        when(ekt.getProducts(1)).thenReturn(page(1,item(1)));
        when(ekt.getProducts(2)).thenReturn(page(2,item(1)));
        long run=sync.sync(false,10,10,0);
        assertThat(syncStore.load(run).listComplete()).isFalse();
        assertThat(jdbc.queryForObject("SELECT reason FROM catalog_sync_runs",String.class)).isEqualTo("REPEATED_PAGE");
        verify(ekt,never()).getProduct(anyLong());
    }
    @Test void detailFailureCanBeRetriedWithoutRepeatingSuccess() throws Exception {
        when(ekt.getProducts(1)).thenReturn(page(1,item(1),item(2)));
        doReturn(page(2)).when(ekt).getProducts(2);
        when(ekt.getProduct(1)).thenThrow(new EktException(HttpStatus.BAD_GATEWAY,"EKT_INVALID_RESPONSE","safe"));
        when(ekt.getProduct(2)).thenReturn(item(2));
        long run=sync.sync(false,10,10,0);
        assertThat(syncStore.remaining(run)).isEqualTo(1);
        doReturn(item(1)).when(ekt).getProduct(1);
        sync.sync(true,0,10,0);
        verify(ekt,times(1)).getProduct(2);
        assertThat(syncStore.remaining(run)).isZero();
    }
    @Test void lockPreventsConcurrentImports() throws Exception {
        try(var connection=jdbc.getDataSource().getConnection(); var statement=connection.createStatement()) {
            statement.execute("SELECT pg_advisory_lock(5152912026)");
            try {
                assertThatThrownBy(()->sync.sync(false,1,1,0)).hasMessage("CATALOG_SYNC_ALREADY_RUNNING");
            } finally { statement.execute("SELECT pg_advisory_unlock(5152912026)"); }
        }
        verifyNoInteractions(ekt);
    }

    @Test void flywayAndHibernateAgreeOnPostgres() {
        products.saveAndFlush(new Product(515291L, "Test", "001_", "00012", new BigDecimal("0.01"), null));
        Product p = products.findById(515291L).orElseThrow();
        assertThat(p.getArticle()).isEqualTo("001_");
        assertThat(p.getPrice()).isEqualByComparingTo("0.01");
        assertThat(p.getQuantity()).isNull();
    }
}
