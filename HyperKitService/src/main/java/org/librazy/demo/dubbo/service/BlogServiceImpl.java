package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
