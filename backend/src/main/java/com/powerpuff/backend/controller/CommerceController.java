package com.powerpuff.backend.controller;

import com.powerpuff.backend.commerce.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommerceController {
    private final SessionService sessions; private final CartService carts;
    public CommerceController(SessionService sessions,CartService carts){this.sessions=sessions;this.carts=carts;}
    public record ProposalRequest(@NotNull UUID requestId,@Min(1) long productId,
            @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal quantity) {}
    public record ConfirmationRequest(@Min(1) int revision) {}
    @PostMapping("/session") public Map<String,Object> session(){return sessions.create();}
    @GetMapping("/cart") public Map<String,Object> cart(@RequestHeader(value="Authorization",required=false) String auth){return carts.cart(sessions.authenticate(auth));}
    @PostMapping("/cart/proposals") public CartService.Proposal propose(@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody ProposalRequest r){return carts.create(sessions.authenticate(auth),r.requestId(),r.productId(),r.quantity());}
    @PostMapping("/cart/proposals/{id}/confirm") public CartService.Confirmation confirm(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable UUID id,@Valid @RequestBody ConfirmationRequest r){return carts.confirm(sessions.authenticate(auth),id,r.revision());}
}
