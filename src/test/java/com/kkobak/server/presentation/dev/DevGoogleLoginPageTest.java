package com.kkobak.server.presentation.dev;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.global.config.AuthProperties;

// 테스트는 local 프로필로 실행되므로(src/test/resources/application.properties) 페이지가 있어야 한다
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DevGoogleLoginPageTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AuthProperties authProperties;

    // 잡는 버그: 로그인 없이 페이지를 못 열거나(보안 설정 누락), 클라이언트 ID 가 안 들어가 Google 버튼이 동작하지 않는 것
    @Test
    void 로컬에서는_로그인_없이_테스트_페이지가_열리고_설정의_클라이언트_ID가_들어간다() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        mockMvc.perform(get("/dev/google-login"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("data-client_id=\"" + authProperties.getGoogleClientId() + "\"")))
                .andExpect(content().string(not(containsString("{{GOOGLE_CLIENT_ID}}"))));
    }

    // 잡는 버그: 개발용 페이지가 API 문서에 섞여 나오는 것
    @Test
    void 테스트_페이지는_API_문서에_나오지_않는다() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/dev/google-login']").doesNotExist());
    }
}
