package com.quectel.web.cloud.salesleadhubserver.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MeController HTTP 契约的离线单元测试。
 *
 * <p>刻意用纯 JUnit + 反射，不加 {@code @SpringBootTest}：本工程硬依赖 MySQL/UAA，
 * 全量上下文测试会因基础设施不可达而假红，不适合作门禁。此测试离线可跑、绿而有效——
 * 谁改动 {@code /me}、{@code /me/id} 的路径或注解即会失败，守住前后端联调契约。
 *
 * <p>业务代码落地后，应按 java-coding TDD 为 service 层补真实单元测试；需要跑
 * 全上下文/DAO 的集成测试再另用 {@code @SpringBootTest} + Testcontainers（依赖可达时）。
 */
class MeControllerTest {

    @Test
    void isRestControllerMappedToMe() {
        assertThat(MeController.class.isAnnotationPresent(RestController.class)).isTrue();
        RequestMapping mapping = MeController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/me");
    }

    @Test
    void currentUserIsGetWithoutSubPath() throws NoSuchMethodException {
        Method method = MeController.class.getMethod("currentUser");
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).isEmpty();
    }

    @Test
    void currentUserIdIsGetMappedToId() throws NoSuchMethodException {
        Method method = MeController.class.getMethod("currentUserId");
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/id");
    }
}
