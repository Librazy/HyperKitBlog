package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.springframework.transaction.annotation.Transactional;

/**
 * 博客服务
 */
public interface BlogService {

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
    void delete(BlogEntryEntity entry);

    /**
     * 新建博文
     *
     * @param author 作者
     * @param entry  博文
     * @return 博文实体
     */
    @Transactional
    BlogEntryEntity create(UserEntity author, BlogEntry entry);

    /**
     * 更新博文
     *
     * @param old   博文实体
     * @param entry 博文
     * @return 博文实体
     */
    @Transactional
    BlogEntryEntity update(BlogEntryEntity old, BlogEntry entry);

}
