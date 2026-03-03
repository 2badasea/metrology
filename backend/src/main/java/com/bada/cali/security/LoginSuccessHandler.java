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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Log4j2
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final MemberServiceImpl memberService;
	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		log.debug("Login success hook called");

		// loadUserByUsername에서 이미 조회·검증된 CustomUserDetails를 Principal에서 직접 사용
		// — loginId로 DB를 재조회하는 중복 쿼리 제거
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String clientIp = Utils.getClientIp(request);

		ResMessage<Object> resMessage = memberService.processLoginSuccess(userDetails, clientIp);

		response.setStatus(200);
		response.setContentType("application/json;charset=utf-8");
		response.getWriter().write(objectMapper.writeValueAsString(resMessage));
	}
}