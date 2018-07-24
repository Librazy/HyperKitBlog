package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;
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
}
