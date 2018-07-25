package org.librazy.demo.dubbo.model;

public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = -8107060015738933030L;

    public BadRequestException() {
    }

    public BadRequestException(String message) {
        super(message);
    }
}
