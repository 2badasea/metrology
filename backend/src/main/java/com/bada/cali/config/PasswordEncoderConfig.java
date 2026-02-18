package com.bada.cali.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder 빈을 별도 설정 클래스로 분리.
 * CustomSecurityConfig → LoginSuccessHandler → MemberServiceImpl → PasswordEncoder → CustomSecurityConfig
 * 로 이어지는 순환 의존성을 차단하기 위함.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
