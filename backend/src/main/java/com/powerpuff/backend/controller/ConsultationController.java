package com.powerpuff.backend.controller;

import com.powerpuff.backend.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ConsultationController {
    private final PurchaseTermsService terms; private final AnalogService analogs;
    public ConsultationController(PurchaseTermsService terms,AnalogService analogs){this.terms=terms;this.analogs=analogs;}
    @GetMapping("/purchase-terms") public JsonNode terms(){return terms.get();}
    @GetMapping("/products/{id}/analogs") public Map<String,Object> analogs(@PathVariable @Min(1) long id){return analogs.find(id);}
}
