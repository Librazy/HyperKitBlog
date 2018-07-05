package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.SrpAccountRepository;
import org.librazy.demo.dubbo.domain.repo.UserRepository;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    private final SrpAccountRepository srpAccountRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, SrpAccountRepository srpAccountRepository) {
        this.userRepository = userRepository;
        this.srpAccountRepository = srpAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        log.debug("Loading user " + id);
        Optional<UserEntity> user = userRepository.findById(Long.valueOf(id));
        if (!user.isPresent()) {
            log.info("user " + id + "not found when loading");
            throw new UsernameNotFoundException("");
        }
        return createUser(user.get().getSrpAccount());
    }

    @Override
    @Transactional
    public UserEntity registerUser(SrpSignupForm signupForm) {
        log.info("Registering new srp user " + signupForm.getEmail());
        UserEntity user = new UserEntity(signupForm.getEmail(), signupForm.getNick());
        new SrpAccountEntity(user, signupForm.getSalt(), signupForm.getVerifier());
        user = userRepository.save(user);
        return user;
    }

    private User createUser(SrpAccountEntity account) {
        return new User(account.getId().toString(), account.getVerifier(),
                createAuthority(account));
    }

    private Collection<GrantedAuthority> createAuthority(SrpAccountEntity account) {
        return account.getUser().getRole().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
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
    public void clear() {
        srpAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }
}
