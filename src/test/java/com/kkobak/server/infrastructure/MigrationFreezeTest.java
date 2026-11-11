package com.kkobak.server.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

// 이미 적용된 Flyway 마이그레이션은 수정하면 안 된다 (프로젝트 규칙). 수정하면 이미 DB 가 있는 환경(개발/운영)에서
// 서버가 "checksum mismatch" 로 아예 뜨지 않는다. 테스트는 항상 빈 DB 에서 시작해서 이 문제를 못 잡으므로 해시로 고정한다.
// 새 변경은 다음 번호(V6, V7, ...)의 새 파일로 추가한다. 새 파일은 DB 에 적용(배포)되는 시점에 이 목록에 해시를 추가해 고정한다.
class MigrationFreezeTest {

    private static final Path MIGRATION_DIRECTORY = Paths.get("src/main/resources/db/migration");

    private static final Map<String, String> APPLIED_MIGRATION_HASHES = new LinkedHashMap<>();

    static {
        APPLIED_MIGRATION_HASHES.put("V1__create_users.sql",
                "be4430a04fbca1fc04635a909fd5a3f4fb89c9aa6a55bc2b3dfed1fb35b6a205");
        APPLIED_MIGRATION_HASHES.put("V2__create_medications_and_schedules.sql",
                "215951a4589babcfbe9cb519a130558d54c53abde196b5a3235a9aad2b208d1e");
        APPLIED_MIGRATION_HASHES.put("V3__create_dose_logs.sql",
                "08fbce331b6dba9e664b6c5d5e16c0803fdb2ae55dca61b826aa0473663802b4");
        APPLIED_MIGRATION_HASHES.put("V4__create_refresh_tokens.sql",
                "edbb678f75b9eb90e882d16f990f0050f4198fb24e09cba8ef098d95e986c0a1");
        APPLIED_MIGRATION_HASHES.put("V5__create_care_relations_and_dose_checked_by.sql",
                "21e1a94861e1bade69e590c8bceda8565d753ccb3ab82bf3530eae74b1dc19d0");
    }

    @Test
    void 이미_적용된_마이그레이션_파일은_내용이_바뀌지_않았다() throws Exception {
        for (Map.Entry<String, String> applied : APPLIED_MIGRATION_HASHES.entrySet()) {
            Path file = MIGRATION_DIRECTORY.resolve(applied.getKey());

            assertThat(file).as("적용된 마이그레이션 %s 가 삭제/이름 변경되었다", applied.getKey()).exists();
            assertThat(sha256Of(file))
                    .as("적용된 마이그레이션 %s 가 수정되었다. 새 버전 파일을 추가해야 한다", applied.getKey())
                    .isEqualTo(applied.getValue());
        }
    }

    private String sha256Of(Path file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
