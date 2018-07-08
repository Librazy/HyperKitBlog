package org.librazy.demo.dubbo.config;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;
import java.security.SecureRandom;

public class SecurityInstanceUtils {

    private SecurityInstanceUtils(){
        throw new UnsupportedOperationException();
    }

    private static SecureRandom strongRandom;

    public static  MessageDigest getSha256() {
        return DigestUtils.getSha256Digest();
    }

    public static MessageDigest getSha512() {
        return DigestUtils.getSha512Digest();
    }
    
    public static SecureRandom getStrongRandom() {
        return strongRandom;
    }

    public static void setStrongRandom(SecureRandom strongRandom) {
        SecurityInstanceUtils.strongRandom = strongRandom;
    }

}
