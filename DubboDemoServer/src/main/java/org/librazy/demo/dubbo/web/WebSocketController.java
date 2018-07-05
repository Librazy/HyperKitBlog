package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketController {

    @MessageMapping("/broadcast")
    @SendTo("/topic/broadcast")
    public ChatMessage broadcast(ChatMessage message, Principal sender) {
        return message.setSender(Long.parseLong(sender.getName()));
    }
}
