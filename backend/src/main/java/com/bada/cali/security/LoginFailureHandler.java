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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Log4j2
public class LoginFailureHandler implements AuthenticationFailureHandler {

	private static final String FAIL_MSG = "아이디 또는 비밀번호가<br>일치하지 않습니다.";

	private final MemberServiceImpl memberService;
	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		log.debug("Login fail hook called");

		String resMsg = FAIL_MSG;
		int resCode = -1;

		String loginId = request.getParameter("username");
		String clientIp = Utils.getClientIp(request);

		// 계정이 차단되어 있거나 로그인 시도 10분 제한이 걸린 경우 → 예외 메시지를 그대로 사용
		if (exception instanceof InternalAuthenticationServiceException && exception.getCause() instanceof LockedException lockedException) {
			resMsg = lockedException.getMessage();
		} else {
			// 자격증명 실패 → 카운트 증가 및 이력 저장은 Service에서 처리
			memberService.processLoginFailure(loginId, clientIp);
		}

		ResMessage<Object> resMessage = new ResMessage<>(resCode, resMsg, null);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json;charset=utf-8");
		response.getWriter().write(objectMapper.writeValueAsString(resMessage));
	}
}