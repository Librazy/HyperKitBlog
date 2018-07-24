package org.librazy.demo.dubbo.model;

public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = -3491879931152121837L;

    public ForbiddenException() {
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
