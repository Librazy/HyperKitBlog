package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.BlogEntrySearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;

    private final ElasticSearchService elasticSearchService;

    private final RecommendationService recommendationService;

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository, ElasticSearchService elasticSearchService, RecommendationService recommendationService) {
        this.blogRepository = blogRepository;
        this.elasticSearchService = elasticSearchService;
        this.recommendationService = recommendationService;
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
        BlogEntryEntity entity = blogRepository.saveAndFlush(blogEntryEntity);
        entry = BlogEntry.fromEntity(entity);
        String simhash = recommendationService.simhash(entry.getContent());
        entity.setSimhash(simhash);
        entity = blogRepository.saveAndFlush(entity);
        entry.setSimhash(simhash);
        elasticSearchService.put(entry);
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
        entry = BlogEntry.fromEntity(entity);
        String simhash = recommendationService.simhash(entry.getContent());
        entry.setSimhash(simhash);
        elasticSearchService.update(entry);
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogEntryEntity> getUserBlogPaged(UserEntity user, Pageable page) {
        return blogRepository.findAllByAuthorOrderByPublish(user, page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogEntryEntity> getUserStarPaged(UserEntity user, Pageable page) {
        return blogRepository.findAllByStargazersContainingOrderByPublish(user, page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogEntryEntity> getBlogPaged(Pageable page) {
        return blogRepository.findAll(page);
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

    @Override
    @Transactional(readOnly = true)
    public Page<BlogEntryEntity> getBlogBetweenPaged(Timestamp start, Timestamp end, Pageable page) {
        return blogRepository.findAllByPublishBetweenOrderByPublish(start, end, page);
    }

    @Override
    public void refresh() {
        blogRepository.findAll().stream().skip(0).forEach(entity -> {
            String simhash;
            try {
                simhash = recommendationService.simhash(entity.getContent());
                entity.setSimhash(simhash);
                long dis = 1532643563000L - 1421446763000L;
                entity.setPublish(Timestamp.from(Instant.ofEpochMilli(Math.abs(new Random().nextLong() % dis) + 1421446763000L)));
                blogRepository.saveAndFlush(entity);
                BlogEntry entry = BlogEntry.fromEntity(entity);
                entry.setSimhash(simhash);
                elasticSearchService.put(entry);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
