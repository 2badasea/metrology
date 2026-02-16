package com.bada.cali.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Remember-Me 관련 설정 (application.properties → security.remember-me.*)
 */
@ConfigurationProperties(prefix = "security.remember-me")
public record RememberMeProperties(String key) {}
