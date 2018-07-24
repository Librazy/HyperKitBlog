package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    @Override
    public void delete(BlogEntryEntity entry) {
        blogRepository.delete(entry);
    }

    @Override
    @Transactional
    public BlogEntryEntity create(UserEntity author, BlogEntry entry) {
        BlogEntryEntity blogEntryEntity = new BlogEntryEntity(author);
        blogEntryEntity.setContent(entry.getContent());
        blogEntryEntity.setTitle(entry.getTitle());
        blogEntryEntity.setPublish(Timestamp.from(Instant.now()));
        return blogRepository.save(blogEntryEntity);
    }

    @Override
    @Transactional
    public BlogEntryEntity update(BlogEntryEntity old, BlogEntry entry) {
        if (entry.getContent() != null) {
            old.setContent(entry.getContent());
        }
        if (entry.getTitle() != null) {
            old.setTitle(entry.getTitle());
        }
        BlogEntryEntity entity = blogRepository.saveAndFlush(old);
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public BlogEntryEntity get(Long id) {
        return blogRepository.getOne(id);
    }
}
