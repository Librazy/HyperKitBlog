package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.*;
import org.librazy.demo.dubbo.service.BlogService;
import org.librazy.demo.dubbo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;
import java.beans.PropertyEditorSupport;

@SuppressWarnings("ThrowableNotThrown")
@ControllerAdvice
public class ControllersAdvice {

    private static Logger logger = LoggerFactory.getLogger(ControllersAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResult> handle(RuntimeException ex) {
        logger.warn("exception in controller: ", ex);
        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (mostSpecificCause.getClass().equals(EntityNotFoundException.class)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        if (mostSpecificCause.getClass().equals(BadRequestException.class)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        if (mostSpecificCause.getClass().equals(UnauthorizedException.class)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        if (mostSpecificCause.getClass().equals(ForbiddenException.class)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        if (mostSpecificCause.getClass().equals(NotFoundException.class)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(MessageResult.from(mostSpecificCause.getMessage()));
        }

        if (mostSpecificCause.getClass().equals(ConflictException.class)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(MessageResult.from(mostSpecificCause.getMessage()));
        }
        throw ex;
    }

    public static void init(WebDataBinder binder, UserService userService, BlogService blogService) {
        binder.registerCustomEditor(UserEntity.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                try {
                    setValue(userService.loadUserByUsername(String.valueOf(text)));
                } catch (UsernameNotFoundException e) {
                    logger.info("binder error when resovling user {}", text);
                    logger.info("exception: {}", e);
                    throw new NotFoundException(text);
                }
            }
        });

        binder.registerCustomEditor(BlogEntryEntity.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(blogService.get(Long.valueOf(text)));
            }
        });
    }
}