package org.librazy.demo.dubbo.config;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Java 随机数/加密套件辅助类
 */
public class SecurityInstanceUtils {

    private SecurityInstanceUtils() {
        throw new UnsupportedOperationException();
    }

    private static SecureRandom strongRandom;

    /**
     * Sha256
     *
     * @return Sha256
     */
    public static  MessageDigest getSha256() {
        return DigestUtils.getSha256Digest();
    }

    /**
     * Sha512
     *
     * @return Sha512
     */
    public static MessageDigest getSha512() {
        return DigestUtils.getSha512Digest();
    }

    /**
     * 强随机数
     *
     * @return 强随机数
     */
    public static SecureRandom getStrongRandom() {
        return strongRandom;
    }

    /**
     * 设置强随机数
     *
     * @param strongRandom 设置强随机数
     */
    public static void setStrongRandom(SecureRandom strongRandom) {
        SecurityInstanceUtils.strongRandom = strongRandom;
    }

}
