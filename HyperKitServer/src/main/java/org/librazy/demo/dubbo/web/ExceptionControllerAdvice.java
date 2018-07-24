package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;

@SuppressWarnings("ThrowableNotThrown")
@ControllerAdvice
public class ExceptionControllerAdvice {

    private static Logger logger = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResult> handle(RuntimeException ex) {
        logger.warn("exception in controller: ", ex);
        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (mostSpecificCause.getClass().equals(EntityNotFoundException.class)) {
            return ResponseEntity.notFound().build();
        }
        if (mostSpecificCause.getClass().equals(UnauthorizedException.class)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        if (mostSpecificCause.getClass().equals(ForbiddenException.class)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        if (mostSpecificCause.getClass().equals(BadRequestException.class)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        if (mostSpecificCause.getClass().equals(ConflictException.class)){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        throw ex;
    }

}