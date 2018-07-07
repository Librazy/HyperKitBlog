package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketController {

    private static Logger logger = LoggerFactory.getLogger(SrpController.class);

    @MessageMapping("/broadcast")
    @SendTo("/topic/broadcast")
    public ChatMessage broadcast(Message<ChatMessage> message, Principal sender) {
        logger.info("Get broadcast from {}", sender.getName());
        return message.getPayload().setSender(Long.parseLong(sender.getName()));
    }
}
