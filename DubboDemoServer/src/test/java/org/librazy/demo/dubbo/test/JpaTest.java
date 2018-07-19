package org.librazy.demo.dubbo.test;

import org.h2.tools.Server;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.librazy.demo.dubbo.config.JpaCryptoConverter;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.SrpAccountRepository;
import org.librazy.demo.dubbo.domain.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.SocketUtils;

import javax.transaction.Transactional;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Rollback
@ActiveProfiles("test-mysql")
class JpaTest {

    @Autowired
    private SrpAccountRepository srpAccountRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        srpAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @Transactional
    @Rollback
    void srpAccountAndUserTest() {
        UserEntity user1 = new UserEntity("18899999998", "user 1");
        new SrpAccountEntity(user1, "some random salt", "verifier");
        UserEntity user2 = new UserEntity("18899999999", "user 2");
        new SrpAccountEntity(user2, "another random salt", "verifier");
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
        assertNotEquals(user1.getId(), user2.getId());

        UserEntity userWithSameEmail = new UserEntity("18899999999", "user 3");
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(userWithSameEmail));

        assertNotNull(JpaCryptoConverter.getAlgorithm());
        assertNotNull(JpaCryptoConverter.getKey());
    }

    @Test
    void JpaCryptoConverterTest() {
        JpaCryptoConverter jpaCryptoConverter = new JpaCryptoConverter();
        String ciphertext = jpaCryptoConverter.convertToDatabaseColumn("sensitive");
        String plain = jpaCryptoConverter.convertToEntityAttribute(ciphertext);
        assertEquals("sensitive", plain);
        assertThrows(IllegalStateException.class, () -> jpaCryptoConverter.convertToDatabaseColumn(null));
        assertThrows(IllegalStateException.class, () -> jpaCryptoConverter.convertToEntityAttribute("random"));
    }
}
