package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;

import java.util.List;

public interface RecommendationService {
    List<BlogEntryEntity> recommendate(BlogEntryEntity entity);
}
