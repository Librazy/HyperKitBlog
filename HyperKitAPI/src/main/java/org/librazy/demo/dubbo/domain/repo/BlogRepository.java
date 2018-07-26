package org.librazy.demo.dubbo.domain.repo;

import org.librazy.demo.dubbo.domain.BlogEntryEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogRepository extends JpaRepository<BlogEntryEntity, Long> {
    Page<BlogEntryEntity> findAllByAuthor(UserEntity author, Pageable page);

    Page<BlogEntryEntity> findAllByStargazersContaining(UserEntity author, Pageable page);
}
