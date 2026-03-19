package com.bada.cali.security;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.Utils;
import com.bada.cali.service.MemberServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final MemberServiceImpl memberService;
	private final ObjectMapper objectMapper;

	// 환경별 어드민 URL (application.properties의 app.admin.url, 기본값: /admin/)
	@Value("${app.admin.url:/admin/}")
	private String adminUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		log.debug("Login success hook called");

		// loadUserByUsername에서 이미 조회·검증된 CustomUserDetails를 Principal에서 직접 사용
		// — loginId로 DB를 재조회하는 중복 쿼리 제거
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String clientIp = Utils.getClientIp(request);

		ResMessage<Object> resMessage = memberService.processLoginSuccess(userDetails, clientIp);

		// ADMIN인 경우 어드민 페이지 URL을 data에 포함
		// — JS에서 redirect 파라미터가 없을 때 이 URL로 이동
		boolean isAdmin = userDetails.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		if (isAdmin) {
			resMessage.setData(Map.of("redirectUrl", adminUrl));
		}

		response.setStatus(200);
		response.setContentType("application/json;charset=utf-8");
		response.getWriter().write(objectMapper.writeValueAsString(resMessage));
	}
}