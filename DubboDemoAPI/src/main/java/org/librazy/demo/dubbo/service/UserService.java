package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

public interface UserService extends UserDetailsService {
    @Override
    @Transactional(readOnly = true)
    UserDetails loadUserByUsername(String id);

    @Transactional
    UserEntity registerUser(SrpSignupForm signupForm);

    @Transactional(readOnly = true)
    SrpAccountEntity getSrpAccount(String email);

    @Transactional(readOnly = true)
    UserEntity findByEmail(String email);

    @Transactional
    void clear();
}
