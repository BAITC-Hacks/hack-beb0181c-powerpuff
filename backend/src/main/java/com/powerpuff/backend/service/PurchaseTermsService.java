package com.powerpuff.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PurchaseTermsService {
    private final ObjectMapper json; private final String path;
    public PurchaseTermsService(ObjectMapper json,@Value("${app.purchase-terms-file:}") String path){this.json=json;this.path=path;}
    public JsonNode get(){
        if(path.isBlank())return json.valueToTree(Map.of("verified",false,"payment","Не уточнено","delivery","Не уточнено","minimumOrder","Не уточнено","message","Условия покупки необходимо уточнить у менеджера EKT."));
        try {
            var p=Path.of(path);
            if(Files.size(p)>65536)throw new IllegalArgumentException();
            var terms=json.readTree(Files.readString(p));
            if(!terms.path("verified").asBoolean() || terms.path("source").asText().isBlank())throw new IllegalArgumentException();
            for(String field:new String[]{"payment","delivery","minimumOrder"})if(!terms.path(field).isTextual())throw new IllegalArgumentException();
            return terms;
        }catch(Exception ex){throw new com.powerpuff.backend.commerce.BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"TERMS_INVALID","Purchase terms configuration is invalid");}
    }
}
