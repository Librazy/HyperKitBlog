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
import java.util.Properties;

/**
 * Encrypts a database column with a secret key. The key should only be know to
 * the webserver.
 */
@Converter
public class JpaCryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM;
    private static final byte[] KEY;
    private static Logger logger = LoggerFactory.getLogger(JpaCryptoConverter.class);

    static {
        try {
            Properties properties = new Properties();
            properties.load(JpaCryptoConverter.class.getClassLoader()
                                                    .getResourceAsStream("application.properties"));
            ALGORITHM = (String) properties.get("encryption.algorithm");
            KEY = ((String) properties.get("encryption.key")).getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String sensitive) {
        Key keySpec = new SecretKeySpec(JpaCryptoConverter.KEY, "AES");
        try {
            final Cipher c = Cipher.getInstance(JpaCryptoConverter.ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, keySpec);
            return new String(Base64.getEncoder().encode(c.doFinal(sensitive.getBytes())), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error converting to database column", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(String sensitive) {
        Key keySpec = new SecretKeySpec(JpaCryptoConverter.KEY, "AES");
        try {
            final Cipher c = Cipher.getInstance(JpaCryptoConverter.ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(c.doFinal(Base64.getDecoder()
                                              .decode(sensitive.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            logger.error("Error converting to entity attribute", e);
            throw new IllegalStateException(e);
        }
    }
}