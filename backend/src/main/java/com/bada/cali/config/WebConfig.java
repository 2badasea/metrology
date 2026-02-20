package com.bada.cali.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	/**
	 * Tomcat은 기본적으로 POST만 multipart/form-data를 파싱함.
	 * PATCH + multipart 요청 처리 순서:
	 *   1. POST로 위장한 래퍼로 Tomcat multipart 파싱
	 *   2. 파싱 결과를 다시 PATCH로 method 복원 → Spring @PatchMapping 라우팅 정상 동작
	 */
	@Bean
	public MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver() {
			@Override
			public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
				if ("PATCH".equalsIgnoreCase(request.getMethod())) {
					final String originalMethod = request.getMethod();
					HttpServletRequestWrapper postFaked = new HttpServletRequestWrapper(request) {
						@Override
						public String getMethod() { return "POST"; }
					};
					MultipartHttpServletRequest parsed = super.resolveMultipart(postFaked);
					return new MethodRestoredMultipartRequest(parsed, originalMethod);
				}
				return super.resolveMultipart(request);
			}
		};
	}

	/**
	 * multipart 데이터는 파싱 결과를 그대로 위임하고, HTTP 메서드만 원래 값으로 복원
	 */
	private static class MethodRestoredMultipartRequest extends HttpServletRequestWrapper
			implements MultipartHttpServletRequest {

		private final MultipartHttpServletRequest delegate;
		private final String method;

		MethodRestoredMultipartRequest(MultipartHttpServletRequest delegate, String method) {
			super(delegate);
			this.delegate = delegate;
			this.method = method;
		}

		@Override
		public String getMethod() { return method; }

		@Override
		public Iterator<String> getFileNames() { return delegate.getFileNames(); }

		@Override
		public MultipartFile getFile(String name) { return delegate.getFile(name); }

		@Override
		public List<MultipartFile> getFiles(String name) { return delegate.getFiles(name); }

		@Override
		public Map<String, MultipartFile> getFileMap() { return delegate.getFileMap(); }

		@Override
		public MultiValueMap<String, MultipartFile> getMultiFileMap() { return delegate.getMultiFileMap(); }

		@Override
		public String getMultipartContentType(String paramOrFileName) { return delegate.getMultipartContentType(paramOrFileName); }

		@Override
		public HttpMethod getRequestMethod() { return HttpMethod.valueOf(method); }

		@Override
		public HttpHeaders getRequestHeaders() { return delegate.getRequestHeaders(); }

		@Override
		public HttpHeaders getMultipartHeaders(String paramOrFileName) { return delegate.getMultipartHeaders(paramOrFileName); }
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// /member/memberModify.css => static/css/member/memberModify.css 로 자동으로 연동되어 로드
		registry.addResourceHandler("/css/**")
				.addResourceLocations("classpath:/static/css/");		// (NOTATION 2번)

		registry.addResourceHandler("/js/**")
				.addResourceLocations("classpath:/static/js/");
	}
}