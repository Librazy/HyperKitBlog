package org.librazy.demo.dubbo.service;

import java.util.List;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.springframework.transaction.annotation.Transactional;

public interface BlogService {

    @Transactional(readOnly = true)
    BlogEntryEntity get(Long id);

    @Transactional
    void delete(BlogEntryEntity entry);

    @Transactional
    BlogEntryEntity create(UserEntity author, BlogEntry blogForm);

    @Transactional
    BlogEntryEntity update(BlogEntry blogForm);
}
