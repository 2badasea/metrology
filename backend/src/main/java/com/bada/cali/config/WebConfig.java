package com.bada.cali.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// /member/memberModify.css => static/css/member/memberModify.css 로 자동으로 연동되어 로드
		registry.addResourceHandler("/css/**")
				.addResourceLocations("classpath:/static/css/");		// (NOTATION 2번)
		
		registry.addResourceHandler("/js/**")
				.addResourceLocations("classpath:/static/js/");
	}
}
