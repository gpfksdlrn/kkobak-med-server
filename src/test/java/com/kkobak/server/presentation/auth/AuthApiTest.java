package com.kkobak.server.presentation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.application.auth.GoogleAccount;
import com.kkobak.server.application.auth.GoogleTokenVerifierPort;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.Cookie;

/**
 * 인증 전체 흐름을 실제 서버 + 실제 DB 로 검증한다.
 * 트랜잭션을 걸지 않고 실제로 커밋한다: 탈취 감지 시 "전부 폐기"가 롤백되지 않고 정말 저장되는지 봐야 하기 때문.
 * 그래서 테스트가 만든 회원은 @AfterEach 에서 직접 지운다 (토큰/약은 CASCADE 로 같이 지워짐).
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, AuthApiTest.FakeGoogleConfig.class})
class AuthApiTest {

    private static final String EMAIL_PREFIX = "auth-api-";
    private static final Pattern REFRESH_COOKIE = Pattern.compile(RefreshTokenCookies.COOKIE_NAME + "=([^;]*)");

    // 진짜 Google 대신 쓰는 가짜 검증기. "google:{sub}:{email}:{name}" 형식의 토큰만 진짜로 본다.
    @TestConfiguration
    static class FakeGoogleConfig {

        @Bean
        @Primary
        GoogleTokenVerifierPort fakeGoogleTokenVerifier() {
            return new GoogleTokenVerifierPort() {
                @Override
                public GoogleAccount verify(String idToken) {
                    String[] parts = idToken.split(":");
                    if (parts.length != 4 || !parts[0].equals("google")) {
                        throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
                    }
                    return new GoogleAccount(parts[1], parts[2], parts[3]);
                }
            };
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from users where email like ?", EMAIL_PREFIX + "%");
    }

    private String googleIdToken(String key) {
        return "google:sub-" + key + ":" + EMAIL_PREFIX + key + "@example.com:사용자" + key;
    }

    private MvcResult login(String key) throws Exception {
        return mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"" + googleIdToken(key) + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String accessTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.accessToken");
    }

    private String refreshTokenOf(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie 헤더").isNotNull();
        Matcher matcher = REFRESH_COOKIE.matcher(setCookie);
        assertThat(matcher.find()).as("리프레시 토큰 쿠키").isTrue();
        return matcher.group(1);
    }

    private MvcResult refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(RefreshTokenCookies.COOKIE_NAME, refreshToken)))
                .andReturn();
    }

    private long countUsers(String key) {
        Long count = jdbcTemplate.queryForObject("select count(*) from users where email = ?", Long.class,
                EMAIL_PREFIX + key + "@example.com");
        return count == null ? 0 : count;
    }

    // 잡는 버그: 리프레시 토큰이 본문에 노출되거나, 쿠키에 HttpOnly/Path/만료가 빠져 자바스크립트로 읽히거나 모든 요청에 실리는 것
    @Test
    void 로그인하면_Access_Token은_본문으로_Refresh_Token은_HttpOnly_쿠키로_준다() throws Exception {
        MvcResult result = login("cookie");
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(JsonPath.<String>read(body, "$.data.tokenType")).isEqualTo("Bearer");
        assertThat(JsonPath.<Integer>read(body, "$.data.expiresIn")).isEqualTo(1800);
        assertThat(JsonPath.<String>read(body, "$.data.user.email")).isEqualTo(EMAIL_PREFIX + "cookie@example.com");
        assertThat(setCookie).contains("HttpOnly").contains("Path=/api/auth").contains("Max-Age=1209600").contains("SameSite=Lax");
        assertThat(body).doesNotContain(refreshTokenOf(result));
    }

    // 잡는 버그: 로그인할 때마다 회원이 새로 생기는 것
    @Test
    void 처음_로그인하면_회원이_생기고_다시_로그인해도_회원은_하나다() throws Exception {
        login("once");
        login("once");

        assertThat(countUsers("once")).isEqualTo(1);
    }

    // 잡는 버그: 발급한 Access Token 이 실제 API 인증에 쓰이지 않거나, 엉뚱한 사용자로 인식되는 것
    @Test
    void 받은_Access_Token으로_내_정보와_약_API를_쓸_수_있다() throws Exception {
        String accessToken = accessTokenOf(login("me"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL_PREFIX + "me@example.com"))
                .andExpect(jsonPath("$.data.name").value("사용자me"));
        mockMvc.perform(get("/api/medications").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    // 잡는 버그: 재발급에 쓴 리프레시 토큰이 폐기되지 않아 계속 재사용되는 것 (교체가 안 됨)
    @Test
    void 재발급하면_새_토큰이_나오고_쓴_Refresh_Token은_다시_못_쓴다() throws Exception {
        String firstRefresh = refreshTokenOf(login("rotate"));

        MvcResult refreshed = refresh(firstRefresh);
        assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);
        String secondRefresh = refreshTokenOf(refreshed);
        String newAccessToken = accessTokenOf(refreshed);

        assertThat(secondRefresh).isNotEqualTo(firstRefresh);
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                .andExpect(status().isOk());
        assertThat(refresh(secondRefresh).getResponse().getStatus()).as("새로 받은 토큰은 쓸 수 있다").isEqualTo(200);
        assertThat(refresh(firstRefresh).getResponse().getStatus()).as("이미 쓴 토큰은 다시 못 쓴다").isEqualTo(401);
    }

    // 잡는 버그: 탈취된 토큰 재사용을 감지해도 "전부 폐기"가 예외와 함께 롤백되어, 훔친 사람이 받은 새 토큰이 살아남는 것
    @Test
    void 이미_쓴_Refresh_Token이_다시_오면_그_사용자의_토큰이_전부_폐기된다() throws Exception {
        String stolen = refreshTokenOf(login("theft"));
        String newerToken = refreshTokenOf(refresh(stolen));   // 정상 사용자가 재발급 (stolen 은 폐기됨)

        MvcResult reuse = refresh(stolen);                       // 누군가 폐기된 토큰을 다시 씀 → 탈취 의심
        assertThat(reuse.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(reuse.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.error.code"))
                .isEqualTo("INVALID_REFRESH_TOKEN");

        // 정상적으로 받았던 최신 토큰도 함께 폐기되어 다시 로그인해야 한다
        assertThat(refresh(newerToken).getResponse().getStatus()).isEqualTo(401);
    }

    // 잡는 버그: 로그아웃해도 리프레시 토큰이 살아 있거나, 쿠키가 지워지지 않는 것
    @Test
    void 로그아웃하면_Refresh_Token이_폐기되고_쿠키가_지워진다() throws Exception {
        String refreshToken = refreshTokenOf(login("logout"));

        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(RefreshTokenCookies.COOKIE_NAME, refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        assertThat(refresh(refreshToken).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void 쿠키_없이_재발급을_요청하면_401이다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    // 잡는 버그: 검증에 실패한 Google 토큰으로 회원이 생기거나 토큰이 발급되는 것
    @Test
    void 잘못된_Google_토큰이면_401이고_회원과_쿠키가_생기지_않는다() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"forged-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_GOOGLE_TOKEN"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    // 잡는 버그: 만료된 Access Token 이 계속 통과하는 것
    @Test
    void 만료된_Access_Token이면_401이다() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").issuedAt(past).expiresAt(past.plusSeconds(60)).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // 잡는 버그: 우리 비밀키가 아닌 키로 서명한 토큰(위조)이 통과하는 것
    @Test
    void 다른_비밀키로_서명한_Access_Token이면_401이다() throws Exception {
        SecretKeySpec otherKey = new SecretKeySpec(
                "attacker-secret-key-at-least-32-bytes-long!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").issuedAt(now).expiresAt(now.plusSeconds(600)).build();
        String forged = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    // 잡는 버그: 허용한 프론트 주소에서 쿠키를 주고받을 수 없거나(credentials), 아무 사이트에서나 API 를 부를 수 있는 것
    @Test
    void 허용한_프론트_주소만_CORS로_쿠키와_함께_호출할_수_있다() throws Exception {
        mockMvc.perform(options("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(options("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    // 잡는 버그: 보안 설정이 "기본 허용"으로 바뀌어, 새 API 에서 로그인 확인을 깜빡하면 누구나 호출할 수 있게 되는 것.
    // 기본은 "전부 막기"이고, 정해 둔 주소(헬스체크, 로그인/재발급/로그아웃, 문서)만 로그인 없이 열린다.
    @Test
    void 로그인_없이는_정해_둔_공개_주소만_열리고_나머지는_기본으로_막힌다() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());

        // 아직 없는 주소도 로그인 없이는 404 가 아니라 401: 컨트롤러에 닿기 전에 보안 필터가 막는다
        mockMvc.perform(get("/api/some-future-api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
