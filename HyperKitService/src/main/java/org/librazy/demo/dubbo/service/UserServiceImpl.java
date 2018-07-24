package org.librazy.demo.dubbo.service;

import com.alibaba.dubbo.config.annotation.Service;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.domain.repo.UserRepository;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.librazy.demo.dubbo.model.UserForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service(interfaceClass = UserService.class)
@Component
public class UserServiceImpl implements UserService {

    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final BlogRepository blogRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BlogRepository blogRepository) {
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity loadUserByUsername(String id) {
        log.debug("Loading user {}", id);
        Optional<UserEntity> user = userRepository.findById(Long.valueOf(id));
        if (!user.isPresent()) {
            log.info("user {} not found when loading", id);
            throw new UsernameNotFoundException("");
        }
        return user.get();
    }

    @Override
    @Transactional
    public UserEntity registerUser(SrpSignupForm signupForm) {
        log.info("Registering new srp user {}", signupForm.getEmail());
        UserEntity user = new UserEntity(signupForm.getEmail(), signupForm.getNick());
        new SrpAccountEntity(user, signupForm.getSalt(), signupForm.getVerifier());
        user = userRepository.saveAndFlush(user);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public SrpAccountEntity getSrpAccount(String email) {
        return userRepository.getAccount(email).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

	@Override
    @Transactional
    public UserEntity update(UserForm userForm) {
		UserEntity old = loadUserByUsername(String.valueOf(userForm.getId()));
        old.setAvatar(userForm.getAvatar());
        old.setBio(userForm.getBio());
        old.setNick(userForm.getNick());
        return userRepository.save(old);
	}

	@Override
    @Transactional
    public void addStarredEntries(UserEntity user, BlogEntryEntity blog) {
		user.addStarredEntries(blog);
		userRepository.save(user);
		blogRepository.save(blog);
	}

	@Override
	public void removeStarredEntries(UserEntity user, BlogEntryEntity blog) {
		user.removeStarredEntries(blog);
		userRepository.save(user);
		blogRepository.save(blog);
	}

	@Override
    @Transactional
    public void addFollowing(UserEntity following, UserEntity followed) {
		following.addFollowing(followed);
		userRepository.save(followed);
		userRepository.save(following);
	}

	@Override
    @Transactional
    public void removeFollowing(UserEntity following, UserEntity followed) {
		following.removeFollowing(followed);
		userRepository.save(followed);
		userRepository.save(following);
	}

}
