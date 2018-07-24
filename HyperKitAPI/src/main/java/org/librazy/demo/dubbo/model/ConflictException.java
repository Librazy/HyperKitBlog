package org.librazy.demo.dubbo.model;

public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = -9138128403241294702L;

    public ConflictException() {
    }

    public ConflictException(String message) {
        super(message);
    }
}
