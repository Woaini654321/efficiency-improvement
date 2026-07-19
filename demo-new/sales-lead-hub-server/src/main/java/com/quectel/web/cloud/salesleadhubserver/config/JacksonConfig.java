package com.quectel.web.cloud.salesleadhubserver.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * 全局 Jackson 附加配置。
 *
 * <p>把 {@code Long} / {@code long} / {@code BigInteger} 统一序列化为字符串，满足前端「ID 全 string」的约定，
 * 规避 JavaScript Number 只能精确表示 2^53 以内整数、而雪花 id 为 64 位 BIGINT 导致的精度丢失问题。</p>
 *
 * <p>本类为<b>附加型</b> {@link Jackson2ObjectMapperBuilderCustomizer}，通过 {@code modulesToInstall}
 * 追加模块而非替换，不覆盖、不冲突 web-starter 既有的 ObjectMapper 配置。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringSerializerCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("LongToStringModule");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            module.addSerializer(BigInteger.class, ToStringSerializer.instance);
            // modulesToInstall 为追加语义，不覆盖 web-starter 已注册的其它模块
            builder.modulesToInstall(module);
        };
    }
}
