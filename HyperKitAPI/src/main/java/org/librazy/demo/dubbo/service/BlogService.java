package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.BlogEntrySearchResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * 博客服务
 */
public interface BlogService {

    /**
     * 搜索博文
     *
     * @param keyword 关键词
     * @return 搜索结果列表
     */
    List<BlogEntrySearchResult> search(String keyword) throws IOException;

    /**
     * 获取博文
     *
     * @param id ID
     * @return 博文实体
     */
    @Transactional(readOnly = true)
    BlogEntryEntity get(Long id);

    /**
     * 删除博文
     *
     * @param entry 博文实体
     */
    @Transactional
    void delete(BlogEntryEntity entry) throws IOException;

    /**
     * 新建博文
     *
     * @param author   作者
     * @param blogForm 博文
     * @return 博文实体
     */
    @Transactional
    BlogEntryEntity create(UserEntity author, BlogEntry blogForm) throws IOException;

    /**
     * 更新博文
     *
     * @param old   博文实体
     * @param entry 博文
     * @return 博文实体
     */
    @Transactional
    BlogEntryEntity update(BlogEntryEntity old, BlogEntry entry) throws IOException;

}
