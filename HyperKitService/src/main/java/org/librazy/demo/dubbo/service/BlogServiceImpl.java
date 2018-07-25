package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.BlogEntrySearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;

    private final ElasticSearchService elasticSearchService;

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository, ElasticSearchService elasticSearchService) {
        this.blogRepository = blogRepository;
        this.elasticSearchService = elasticSearchService;
    }

    @Override
    public void delete(BlogEntryEntity entry) throws IOException {
        blogRepository.delete(entry);
        elasticSearchService.delete(entry.getId());
    }

    @Override
    @Transactional
    public BlogEntryEntity create(UserEntity author, BlogEntry entry) throws IOException {
        BlogEntryEntity blogEntryEntity = new BlogEntryEntity(author);
        blogEntryEntity.setContent(entry.getContent());
        blogEntryEntity.setTitle(entry.getTitle());
        blogEntryEntity.setPublish(Timestamp.from(Instant.now()));
        BlogEntryEntity entity = blogRepository.save(blogEntryEntity);
        elasticSearchService.put(BlogEntry.fromEntity(entity));
        return entity;
    }

    @Override
    @Transactional
    public BlogEntryEntity update(BlogEntryEntity old, BlogEntry entry) throws IOException {
        if (entry.getContent() != null) {
            old.setContent(entry.getContent());
        }
        if (entry.getTitle() != null) {
            old.setTitle(entry.getTitle());
        }
        BlogEntryEntity entity = blogRepository.saveAndFlush(old);
        elasticSearchService.put(BlogEntry.fromEntity(entity));
        return entity;
    }

    @Override
    public List<BlogEntrySearchResult> search(String keyword) throws IOException {
        return elasticSearchService.search(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogEntryEntity get(Long id) {
        return blogRepository.getOne(id);
    }
}
