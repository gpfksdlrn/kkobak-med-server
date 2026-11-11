package com.kkobak.server.global.exception;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 예외를 일부러 던지는 가짜 컨트롤러로, 핸들러가 예외마다 올바른 상태/코드를 내려주는지 본다.
// (실제 API 로는 "동시 요청으로 UNIQUE 위반"이나 "예상 못한 예외"를 결정적으로 만들 수 없다)
class GlobalExceptionHandlerTest {

    private static final String SECRET_MESSAGE = "jdbc:postgresql://내부-호스트/비밀-테이블-이름";

    @RestController
    @RequestMapping("/test")
    static class ThrowingController {

        @GetMapping("/duplicate")
        public String duplicate() {
            throw new DataIntegrityViolationException(SECRET_MESSAGE);
        }

        @GetMapping("/unexpected")
        public String unexpected() {
            throw new IllegalStateException(SECRET_MESSAGE);
        }

        @GetMapping("/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException(SECRET_MESSAGE);
        }

        @GetMapping("/business")
        public String business() {
            throw new BusinessException(ErrorCode.MEDICATION_NOT_FOUND);
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void DB_제약_위반은_409_CONFLICT로_바뀌고_DB_내부_메시지는_숨긴다() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value(not(containsString("jdbc"))));
    }

    @Test
    void 예상하지_못한_예외는_500_INTERNAL_ERROR이고_예외_메시지가_응답에_새지_않는다() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    void 업무_예외는_ErrorCode에_정해진_상태와_코드로_응답한다() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEDICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("약을 찾을 수 없습니다."));
    }

    // 잡는 버그: 라이브러리/버그에서 난 IllegalArgumentException 이 400 으로, 내부 메시지와 함께 클라이언트에 새는 것
    @Test
    void IllegalArgumentException은_500이고_내부_메시지가_응답에_새지_않는다() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다."));
    }
}
