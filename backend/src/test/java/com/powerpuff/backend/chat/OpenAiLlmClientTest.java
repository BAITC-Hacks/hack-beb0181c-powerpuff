package com.powerpuff.backend.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerpuff.backend.commerce.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class OpenAiLlmClientTest {
    @Test void responsesFunctionCallIsParsedAndStoreIsDisabled() throws Exception {
        var builder=RestClient.builder().baseUrl("https://api.openai.com/v1");
        var server=MockRestServiceServer.bindTo(builder).build();
        var client=new OpenAiLlmClient(new ObjectMapper(),"fixture","fixture-model",builder.build());
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(header("Authorization","Bearer fixture"))
            .andExpect(jsonPath("$.store").value(false))
            .andExpect(jsonPath("$.tool_choice.name").value("catalog_action"))
            .andRespond(withSuccess(new ObjectMapper().writeValueAsString(java.util.Map.of(
                "status","completed","output",List.of(java.util.Map.of("type","function_call","name","catalog_action",
                "arguments",new ObjectMapper().writeValueAsString(java.util.Map.of("action","search","query","Legrand"))))
                )),MediaType.APPLICATION_JSON));
        assertThat(client.route("Найди Legrand",List.of()).query()).isEqualTo("Legrand");
        server.verify();
    }
    @Test void missingKeyIsExplicit(){
        var client=new OpenAiLlmClient(new ObjectMapper(),"","",RestClient.create());
        assertThatThrownBy(()->client.route("test",List.of())).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.code).isEqualTo("LLM_NOT_CONFIGURED"));
    }
}
