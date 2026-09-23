package com.powerpuff.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.powerpuff.backend.chat.ChatService;
import com.powerpuff.backend.commerce.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chat;private final SessionService sessions;
    public ChatController(ChatService chat,SessionService sessions){this.chat=chat;this.sessions=sessions;}
    public record Message(@NotNull UUID messageId,@NotBlank @Size(max=4000) String text) {}
    @PostMapping public JsonNode send(@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody Message message){return chat.send(sessions.authenticate(auth),message.messageId(),message.text());}
}
