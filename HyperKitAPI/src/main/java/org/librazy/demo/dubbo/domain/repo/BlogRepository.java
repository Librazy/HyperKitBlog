package org.librazy.demo.dubbo.domain.repo;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<BlogEntryEntity, Long> {

    /*Optional<BlogEntryEntity> findById(Long id);
	
	default Optional<SrpAccountEntity> getAccount(String email) {
        return findByEmail(email).map(UserEntity::getSrpAccount);
    }*/
}
