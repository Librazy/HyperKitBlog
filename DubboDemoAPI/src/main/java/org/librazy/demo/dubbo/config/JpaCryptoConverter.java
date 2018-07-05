package org.librazy.demo.dubbo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.security.Key;
import java.util.Base64;
import java.util.Properties;

/**
 * Encrypts a database column with a secret key. The key should only be know to
 * the webserver.
 */
@Converter
public class JpaCryptoConverter implements AttributeConverter<String, String> {

    private static final String algorithm_property_key = "encryption.algorithm";

    private static final String secret_property_key = "encryption.key";

    private static final Properties properties = new Properties();

    private static Logger logger = LoggerFactory.getLogger(JpaCryptoConverter.class);

    private static String ALGORITHM;

    private static byte[] KEY;

    static {
        try {
            properties.load(JpaCryptoConverter.class.getClassLoader()
                                                    .getResourceAsStream("persistence.properties"));
        } catch (Exception e) {
            logger.warn("Could not load properties file 'persistence.properties' using unsecure encryption key.");
            properties.put(algorithm_property_key, "AES/ECB/PKCS5Padding");
            properties.put(secret_property_key, "MySuperSecretKey");
        }
        ALGORITHM = (String) properties.get(algorithm_property_key);
        KEY = ((String) properties.get(secret_property_key)).getBytes();
    }

    public String convertToDatabaseColumn(String sensitive) {
        Key key = new SecretKeySpec(KEY, "AES");
        try {
            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, key);
            return new String(Base64.getEncoder().encode(c
                                                                 .doFinal(sensitive.getBytes())), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String convertToEntityAttribute(String sensitive) {
        Key key = new SecretKeySpec(KEY, "AES");
        try {
            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            return new String(c.doFinal(Base64.getDecoder()
                                              .decode(sensitive.getBytes("UTF-8"))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}