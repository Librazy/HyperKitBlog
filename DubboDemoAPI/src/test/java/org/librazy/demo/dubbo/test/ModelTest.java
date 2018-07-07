package org.librazy.demo.dubbo.test;

import org.junit.jupiter.api.Test;
import org.librazy.demo.dubbo.config.JpaCryptoConverter;
import org.librazy.demo.dubbo.model.ChatMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelTest {

    @Test
    void ChatMessageTest() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent("Content");
        assertEquals("Content", chatMessage.getContent());
    }

    @Test
    void JpaCryptoConverterTest(){
        JpaCryptoConverter jpaCryptoConverter = new JpaCryptoConverter();
        String ciphertext = jpaCryptoConverter.convertToDatabaseColumn("sensitive");
        String plain = jpaCryptoConverter.convertToEntityAttribute(ciphertext);
        assertEquals("sensitive", plain);
        assertThrows(IllegalStateException.class, () -> jpaCryptoConverter.convertToDatabaseColumn(null));
        assertThrows(IllegalStateException.class, () -> jpaCryptoConverter.convertToEntityAttribute("random"));
    }
}
