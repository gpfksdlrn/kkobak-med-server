package com.kkobak.server.infrastructure;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.kkobak.server.TestcontainersConfiguration;

import jakarta.persistence.EntityManagerFactory;

// 운영 설정은 ddl-auto: none 이라서, Entity 와 Flyway 가 만든 테이블이 어긋나도 서버가 조용히 뜬다
// (어긋난 컬럼을 처음 쓰는 요청에서야 터진다). 여기서는 Hibernate 의 스키마 검증을 직접 실행해서 어긋남을 잡는다.
// 어긋나면 SchemaManagementException 이 던져져 테스트가 실패한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaMatchesEntitiesTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void 모든_Entity의_테이블과_컬럼이_마이그레이션이_만든_스키마와_일치한다() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

        sessionFactory.getSchemaManager().validateMappedObjects();
    }
}
