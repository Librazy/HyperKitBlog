package org.librazy.demo.dubbo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;


/**
 * 数据库列加密。
 */
@Converter
public class JpaCryptoConverter implements AttributeConverter<String, String> {

    private static String algorithm;

    private static byte[] key;

    private static Logger logger = LoggerFactory.getLogger(JpaCryptoConverter.class);

    /**
     * 获取加密算法
     *
     * @return 算法
     */
    public static String getAlgorithm() {
        return algorithm;
    }

    /**
     * 设置算法
     *
     * @param algorithm 算法
     */
    public static void setAlgorithm(String algorithm) {
        JpaCryptoConverter.algorithm = algorithm;
    }

    /**
     * 获取加密密钥
     *
     * @return the byte [ ]
     */
    public static byte[] getKey() {
        return key;
    }

    /**
     * 设置密钥
     *
     * @param key the key
     */
    public static void setKey(byte[] key) {
        JpaCryptoConverter.key = key;
    }

    @Override
    public String convertToDatabaseColumn(String sensitive) {
        Key keySpec = new SecretKeySpec(JpaCryptoConverter.key, "AES");
        try {
            final Cipher c = Cipher.getInstance(JpaCryptoConverter.algorithm);
            c.init(Cipher.ENCRYPT_MODE, keySpec);
            return new String(Base64.getEncoder().encode(c.doFinal(sensitive.getBytes())), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error converting to database column", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(String sensitive) {
        Key keySpec = new SecretKeySpec(JpaCryptoConverter.key, "AES");
        try {
            final Cipher c = Cipher.getInstance(JpaCryptoConverter.algorithm);
            c.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(c.doFinal(Base64.getDecoder()
                                              .decode(sensitive.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            logger.error("Error converting to entity attribute", e);
            throw new IllegalStateException(e);
        }
    }
}