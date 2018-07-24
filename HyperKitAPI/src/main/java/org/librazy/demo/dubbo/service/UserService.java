package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.librazy.demo.dubbo.model.UserForm;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

public interface UserService extends UserDetailsService {
    @Override
    @Transactional(readOnly = true)
    UserEntity loadUserByUsername(String id);

    @Transactional
    UserEntity registerUser(SrpSignupForm signupForm);

    @Transactional(readOnly = true)
    SrpAccountEntity getSrpAccount(String email);

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
