package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class BlogServiceImpl implements BlogService {

    private static Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

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
    public BlogEntryEntity create(UserEntity author, BlogEntry blogForm) {
        BlogEntryEntity blogEntryEntity = new BlogEntryEntity(author);
        blogEntryEntity.setContent(blogForm.getContent());
        blogEntryEntity.setTitle(blogForm.getTitle());
        return blogRepository.save(blogEntryEntity);
    }

    @Override
    @Transactional
    public BlogEntryEntity update(BlogEntry blogForm) {
        BlogEntryEntity old = get(blogForm.getId());
        old.setContent(blogForm.getContent());
        old.setTitle(blogForm.getTitle());
        return blogRepository.save(old);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogEntryEntity get(Long id) {
        return blogRepository.getOne(id);
    }
}
