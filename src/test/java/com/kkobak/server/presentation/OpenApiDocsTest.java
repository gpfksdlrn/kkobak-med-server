package com.kkobak.server.presentation;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.presentation.auth.AuthController;
import com.kkobak.server.presentation.care.CareRelationController;
import com.kkobak.server.presentation.doselog.DoseLogController;
import com.kkobak.server.presentation.health.HealthController;
import com.kkobak.server.presentation.medication.MedicationController;
import com.kkobak.server.presentation.user.UserController;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OpenApiDocsTest {

    // 문서에 나오는(XxxApiSpec 이 있는) 컨트롤러 전부. 새 컨트롤러를 만들면 여기에도 추가한다.
    // DevGoogleLoginPageController 는 로컬 전용이고 문서에서 제외(springdoc.paths-to-exclude)라 넣지 않는다.
    private static final List<Class<?>> CONTROLLERS = Arrays.<Class<?>>asList(
            HealthController.class, MedicationController.class, DoseLogController.class,
            AuthController.class, CareRelationController.class, UserController.class);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void 명세_인터페이스의_내용이_Swagger_문서에_반영된다() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("꼬박약 API"))
                .andExpect(jsonPath("$.paths['/api/health'].get.summary").value("서버 상태 확인"))
                .andExpect(jsonPath("$.paths['/api/medications'].post.summary").value("약 등록"))
                .andExpect(jsonPath("$.paths['/api/medications'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/medications/{medicationId}'].get.responses['404'].content['application/json'].example.error.code")
                        .value("MEDICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/api/dose-logs'].put.summary").value("복용 체크"))
                .andExpect(jsonPath("$.paths['/api/dose-logs'].put.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/dose-logs'].put.responses['409']").doesNotExist());
    }

    // 규칙: 문서용 어노테이션은 XxxApiSpec 인터페이스에만 쓰고, 운영 코드(Controller)에는 쓰지 않는다.
    @Test
    void 컨트롤러에는_Swagger_어노테이션이_없다() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Annotation annotation : controller.getDeclaredAnnotations()) {
                if (isSwaggerAnnotation(annotation)) {
                    violations.add(controller.getSimpleName() + " 클래스: " + annotation.annotationType().getSimpleName());
                }
            }
            for (Method method : controller.getDeclaredMethods()) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (isSwaggerAnnotation(annotation)) {
                        violations.add(controller.getSimpleName() + "." + method.getName() + ": "
                                + annotation.annotationType().getSimpleName());
                    }
                }
                for (Parameter parameter : method.getParameters()) {
                    for (Annotation annotation : parameter.getDeclaredAnnotations()) {
                        if (isSwaggerAnnotation(annotation)) {
                            violations.add(controller.getSimpleName() + "." + method.getName() + " 파라미터: "
                                    + annotation.annotationType().getSimpleName());
                        }
                    }
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    private boolean isSwaggerAnnotation(Annotation annotation) {
        return annotation.annotationType().getName().startsWith("io.swagger");
    }
}
