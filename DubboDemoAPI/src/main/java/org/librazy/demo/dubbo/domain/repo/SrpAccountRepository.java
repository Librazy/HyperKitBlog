package org.librazy.demo.dubbo.domain.repo;

import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SrpAccountRepository extends JpaRepository<SrpAccountEntity, Long> {

}
