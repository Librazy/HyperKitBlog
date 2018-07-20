package org.librazy.demo.dubbo.service;

import java.util.List;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.SrpBlogForm;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

public interface BlogService {

    @Transactional(readOnly = true)
    BlogEntryEntity getBlogById(Long id);

    @Transactional
    Boolean deleteBlogById(Long id);

    @Transactional
    Boolean createBlog(SrpBlogForm blogForm);
    
    @Transactional
    Boolean updateBlog(SrpBlogForm blogForm);
    
    @Transactional(readOnly = true)
    List<UserEntity> listBlogStargazers(Long id);
    
}
