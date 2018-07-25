package org.librazy.demo.dubbo.model;

public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1439976496189071373L;

    public NotFoundException() {
    }

    public NotFoundException(String message) {
        super(message);
    }
}
