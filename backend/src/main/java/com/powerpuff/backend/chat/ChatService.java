package com.powerpuff.backend.chat;

import com.fasterxml.jackson.databind.*;
import com.powerpuff.backend.commerce.*;
import com.powerpuff.backend.service.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ChatService {
    private final JdbcTemplate db;private final TransactionTemplate tx;private final SessionService sessions;
    private final LlmClient llm;private final CartService cart;private final CatalogSearchService search;
    private final CatalogService catalog;private final AnalogService analogs;private final PurchaseTermsService terms;private final ObjectMapper json;
    public ChatService(JdbcTemplate db,TransactionTemplate tx,SessionService sessions,LlmClient llm,CartService cart,CatalogSearchService search,CatalogService catalog,AnalogService analogs,PurchaseTermsService terms,ObjectMapper json){
        this.db=db;this.tx=tx;this.sessions=sessions;this.llm=llm;this.cart=cart;this.search=search;this.catalog=catalog;this.analogs=analogs;this.terms=terms;this.json=json;
    }
    public JsonNode send(UUID session,UUID messageId,String raw){
        // Платёжные номера не нужны каталогу; не передаём и не сохраняем их.
        String text=raw.replaceAll("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)","[скрыто]");
        JsonNode cached=tx.execute(s->{
            sessions.lock(session);
            var rows=db.queryForList("SELECT user_text,response FROM chat_messages WHERE session_id=? AND request_id=?",session,messageId);
            if(!rows.isEmpty()){
                if(!rows.getFirst().get("user_text").equals(text))throw conflict("MESSAGE_ID_REUSED");
                if(rows.getFirst().get("response")==null)throw conflict("MESSAGE_IN_PROGRESS");
                try{return json.readTree(rows.getFirst().get("response").toString());}catch(Exception ex){throw new IllegalStateException();}
            }
            if(db.queryForObject("SELECT count(*) FROM chat_messages WHERE session_id=?",Long.class,session)>=100)throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,"SESSION_MESSAGE_LIMIT","Создайте новую сессию");
            db.update("INSERT INTO chat_messages(session_id,request_id,user_text) VALUES (?,?,?)",session,messageId,text);
            return null;
        });
        if(cached!=null)return cached;
        try {
            Object data;String answer;
            if(Set.of("да, добавь","да добавь").contains(text.strip().toLowerCase(Locale.ROOT))){
                var pending=cart.pending(session);var confirmed=cart.confirm(session,pending.id(),pending.revision());
                data=confirmed;answer=confirmed.requiresConfirmation()?"Цена или остаток изменились. Проверьте новое предложение и подтвердите ещё раз.":"Товар добавлен в корзину.";
            } else {
                var history=new ArrayList<Map<String,String>>();
                var previous=db.queryForList("SELECT user_text,response FROM chat_messages WHERE session_id=? AND response IS NOT NULL ORDER BY created_at DESC LIMIT 5",session);
                Collections.reverse(previous);
                for(var row:previous){history.add(Map.of("role","user","content",row.get("user_text").toString()));String result=row.get("response").toString();history.add(Map.of("role","assistant","content",result.substring(0,Math.min(8000,result.length()))));}
                var action=llm.route(text,history);
                switch(action.action()) {
                    case "search" -> {data=search.search(action.query(),0,5);answer="Результаты поиска в загруженном каталоге. Уточните товар и количество.";}
                    case "product" -> {var p=catalog.get(productId(action));data=p;answer=p.name()+". Цена: "+(p.price()==null?"не уточнена":p.price())+". Остаток по данным EKT: "+(p.quantity()==null?"не уточнён":p.quantity())+". Характеристики и ссылки — в карточке.";}
                    case "analogs" -> {data=analogs.find(productId(action));answer="Кандидаты подбираются по характеристикам. Проверьте объяснение и ограничения.";}
                    case "terms" -> {data=terms.get();answer=terms.get().path("verified").asBoolean()?"Условия покупки приведены в ответе из настроенного источника.":"Условия оплаты, доставки и минимальной партии пока не уточнены. Обратитесь к менеджеру EKT.";}
                    case "propose" -> {if(action.quantity()==null)throw conflict("QUANTITY_REQUIRED");data=cart.create(session,messageId,productId(action),new BigDecimal(action.quantity()));answer="Проверьте товар, количество и цену. Для добавления подтвердите предложение кнопкой или напишите «да, добавь». Корзина пока не изменена.";}
                    default -> {data=Map.of();answer="Уточните артикул, название товара или вопрос об условиях покупки.";}
                }
            }
            var result=json.valueToTree(Map.of("messageId",messageId,"text",answer,"data",data));
            db.update("UPDATE chat_messages SET response=CAST(? AS JSONB) WHERE session_id=? AND request_id=?",result.toString(),session,messageId);
            return result;
        }catch(RuntimeException ex){db.update("DELETE FROM chat_messages WHERE session_id=? AND request_id=? AND response IS NULL",session,messageId);throw ex;}
    }
    private long productId(LlmClient.Action a){if(a.productId()==null || a.productId()<1)throw conflict("PRODUCT_REQUIRED");return a.productId();}
    private BusinessException conflict(String code){return new BusinessException(HttpStatus.CONFLICT,code,"Уточните запрос или дождитесь завершения предыдущего сообщения");}
}
