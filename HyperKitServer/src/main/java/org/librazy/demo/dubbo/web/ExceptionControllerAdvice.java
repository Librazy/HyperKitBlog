package org.librazy.demo.dubbo.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;

@SuppressWarnings("ThrowableNotThrown")
@ControllerAdvice
public class ExceptionControllerAdvice {

    private static Logger logger = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handle(RuntimeException ex) {
        logger.warn("exception in controller: ", ex);
        if(NestedExceptionUtils.getMostSpecificCause(ex).getClass().equals(EntityNotFoundException.class)){
            return ResponseEntity.notFound().build();
        }
        throw ex;
    }

}