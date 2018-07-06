package org.librazy.demo.dubbo.test;

import org.junit.jupiter.api.Test;
import org.librazy.demo.dubbo.model.ChatMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelTest {

    @Test
    void ChatMessageTest(){
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent("Content");
        assertEquals("Content", chatMessage.getContent());
    }
}
