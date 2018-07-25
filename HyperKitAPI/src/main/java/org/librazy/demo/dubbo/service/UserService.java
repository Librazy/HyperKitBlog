package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.librazy.demo.dubbo.model.UserForm;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务
 */
public interface UserService extends UserDetailsService {

    @Override
    @Transactional(readOnly = true)
    UserEntity loadUserByUsername(String id);

    /**
     * 注册用户
     *
     * @param signupForm 注册表单
     * @return 用户实体
     */
    @Transactional
    UserEntity registerUser(SrpSignupForm signupForm);

    /**
     * 获取关联的 SRP 账号实体
     *
     * @param email 邮箱
     * @return SRP 账号实体
     */
    @Transactional(readOnly = true)
    SrpAccountEntity getSrpAccount(String email);

    /**
     * 根据邮箱查找用户实体
     *
     * @param email 邮箱
     * @return 用户实体
     */
    @Transactional(readOnly = true)
    UserEntity findByEmail(String email);

    @Transactional
    UserEntity update(UserForm userForm);

    @Transactional
    boolean addStarredEntries(UserEntity user, BlogEntryEntity blog);

    @Transactional
    boolean removeStarredEntries(UserEntity user, BlogEntryEntity blog);

    @Transactional
    boolean addFollowing(UserEntity follower, UserEntity following);

    @Transactional
    boolean removeFollowing(UserEntity follower, UserEntity following);
}
