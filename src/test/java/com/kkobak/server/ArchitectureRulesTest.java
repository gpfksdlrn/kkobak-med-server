package com.kkobak.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

// 프로젝트 구조 규칙을 코드로 고정한다. 규칙을 어긴 import/어노테이션이 한 줄이라도 생기면 어느 파일 몇 번째 줄인지 알려준다.
// (ArchUnit 같은 라이브러리를 새로 들이지 않고, 소스 파일의 import 줄을 직접 읽는다)
class ArchitectureRulesTest {

    private static final Path MAIN_JAVA = Paths.get("src/main/java/com/kkobak/server");
    private static final Path MAIN_RESOURCES = Paths.get("src/main/resources");

    // 계층 폴더 안의 모든 .java 파일을 모은다
    private List<Path> javaFilesUnder(String layer) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(MAIN_JAVA.resolve(layer))) {
            walk.filter(path -> path.toString().endsWith(".java")).forEach(files::add);
        }
        return files;
    }

    private List<Path> allJavaFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(MAIN_JAVA)) {
            walk.filter(path -> path.toString().endsWith(".java")).forEach(files::add);
        }
        return files;
    }

    // "파일:줄 내용" 형태로, 금지된 문자열이 들어간 줄을 모두 찾는다 (주석 줄은 제외)
    private List<String> findLinesContaining(List<Path> files, String... forbiddenTexts) throws IOException {
        List<String> found = new ArrayList<>();
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                    continue;
                }
                for (String forbiddenText : forbiddenTexts) {
                    if (line.contains(forbiddenText)) {
                        found.add(file.getFileName() + ":" + (i + 1) + " " + line);
                    }
                }
            }
        }
        return found;
    }

    @Test
    void domain은_application_presentation_infrastructure를_import하지_않는다() throws IOException {
        List<String> violations = findLinesContaining(javaFilesUnder("domain"),
                "import com.kkobak.server.application", "import com.kkobak.server.presentation",
                "import com.kkobak.server.infrastructure");

        assertThat(violations).isEmpty();
    }

    @Test
    void application은_presentation과_infrastructure를_import하지_않는다() throws IOException {
        List<String> violations = findLinesContaining(javaFilesUnder("application"),
                "import com.kkobak.server.presentation", "import com.kkobak.server.infrastructure");

        assertThat(violations).isEmpty();
    }

    // Controller 는 Facade 만 부른다: Service, Repository, Port, infrastructure 를 모른다
    @Test
    void presentation은_infrastructure와_Service_Repository_Port를_import하지_않는다() throws IOException {
        List<String> violations = findLinesContaining(javaFilesUnder("presentation"),
                "import com.kkobak.server.infrastructure", "Service;", "Repository;", "Port;");

        assertThat(violations).isEmpty();
    }

    @Test
    void infrastructure의_JpaRepository는_package_private이다() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFilesUnder("infrastructure")) {
            if (file.getFileName().toString().endsWith("JpaRepository.java")) {
                violations.addAll(findLinesContaining(List.of(file), "public interface"));
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void Transactional은_Facade에만_있다() throws IOException {
        List<Path> notFacadeFiles = new ArrayList<>();
        for (Path file : allJavaFiles()) {
            if (!file.getFileName().toString().endsWith("Facade.java")) {
                notFacadeFiles.add(file);
            }
        }

        assertThat(findLinesContaining(notFacadeFiles, "@Transactional")).isEmpty();
    }

    @Test
    void 운영_코드는_현재_시각을_직접_꺼내지_않고_Clock을_주입받는다() throws IOException {
        List<Path> notClockConfigFiles = new ArrayList<>();
        for (Path file : allJavaFiles()) {
            if (!file.getFileName().toString().equals("ClockConfig.java")) {
                notClockConfigFiles.add(file);
            }
        }

        assertThat(findLinesContaining(notClockConfigFiles, "Instant.now(", "LocalDate.now(", "LocalDateTime.now(",
                "LocalTime.now(", "ZonedDateTime.now(", "Clock.system", "System.currentTimeMillis("))
                .as("시각을 직접 꺼내면 테스트에서 고정할 수 없다. Clock 을 주입받아 clock.instant() 를 쓴다")
                .isEmpty();
    }

    @Test
    void Entity에는_Data_Setter_Builder가_없다() throws IOException {
        List<Path> entityFiles = new ArrayList<>();
        for (Path file : javaFilesUnder("domain")) {
            if (Files.readString(file, StandardCharsets.UTF_8).contains("@Entity")) {
                entityFiles.add(file);
            }
        }

        assertThat(entityFiles).as("Entity 파일을 찾지 못했다").isNotEmpty();
        assertThat(findLinesContaining(entityFiles, "@Data", "@Setter", "@Builder", "@AllArgsConstructor")).isEmpty();
    }

    @Test
    void 필드_Autowired_없이_생성자로_주입한다() throws IOException {
        assertThat(findLinesContaining(allJavaFiles(), "@Autowired")).isEmpty();
    }

    // 프로젝트 규칙: 문서용 어노테이션은 XxxApiSpec 에만. DTO(Request/Response)에 @Schema 도 붙이지 않는다.
    @Test
    void Swagger_어노테이션은_ApiSpec_인터페이스에만_있다() throws IOException {
        List<Path> notApiSpecFiles = new ArrayList<>();
        for (Path file : allJavaFiles()) {
            String fileName = file.getFileName().toString();
            boolean isAllowed = fileName.endsWith("ApiSpec.java") || fileName.equals("OpenApiConfig.java");
            if (!isAllowed) {
                notApiSpecFiles.add(file);
            }
        }

        assertThat(findLinesContaining(notApiSpecFiles, "io.swagger.v3.oas.annotations")).isEmpty();
    }

    // 회귀(버그 노트 1번): 이 설정이 있으면 LocalTime 이 변환되어 08:00 이 23:00 으로 저장된다
    @Test
    void 설정_파일에_hibernate_jdbc_time_zone이_없다() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(MAIN_RESOURCES)) {
            List<Path> configFiles = new ArrayList<>();
            walk.filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".properties"))
                    .forEach(configFiles::add);
            assertThat(configFiles).as("설정 파일을 찾지 못했다").isNotEmpty();
            for (Path file : configFiles) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    boolean isComment = line.trim().startsWith("#");
                    if (!isComment && line.contains("time_zone")) {
                        violations.add(file.getFileName() + " " + line.trim());
                    }
                }
            }
        }

        assertThat(violations).isEmpty();
    }
}
