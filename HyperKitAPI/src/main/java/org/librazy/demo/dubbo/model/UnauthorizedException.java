package org.librazy.demo.dubbo.model;

public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 4266965647168130980L;

    public UnauthorizedException() {
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
