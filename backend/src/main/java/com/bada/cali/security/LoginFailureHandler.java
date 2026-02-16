package com.bada.cali.security;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.Utils;
import com.bada.cali.entity.Log;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Member;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.MemberRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Log4j2
public class LoginFailureHandler implements AuthenticationFailureHandler {
	
	private static final String FAIL_MSG = "아이디 또는 비밀번호가<br>일치하지 않습니다.";
	private final MemberRepository memberRepository;
	private final LogRepository logRepository;
	private final ObjectMapper objectMapper;
	
	/**
	 * 로그인 실패에 대한 훅
	 *
	 * @param request
	 * @param response
	 * @param exception
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		log.debug("Login fail hook called");

		String resMsg = FAIL_MSG;
		int resCode = -1;

		String loginId = request.getParameter("username");
		String clientIp = Utils.getClientIp(request);

		// 계정이 차단되어 있거나 로그인 시도 10분 제한이 걸린 경우
		if (exception instanceof InternalAuthenticationServiceException && exception.getCause() instanceof LockedException lockedException) {
			resMsg = lockedException.getMessage();
		} else {
			Optional<Member> optLoginMember = memberRepository.findByLoginId(loginId, YnType.y);
			if (optLoginMember.isPresent()) {
				Member tryLoginMember = optLoginMember.get();
				// 로그인 실패 시 시도 횟수 증가 (5회 도달 시 10분 잠금 → CustomUserDetailService에서 체크)
				// count 0~4: +1 증가, count 5: 잠금 해제 후 재실패이므로 1로 리셋하여 새 카운트 시작
				if (tryLoginMember.getLoginCount() <= 5) {
					int updateCountValue = (tryLoginMember.getLoginCount() <= 4) ? (tryLoginMember.getLoginCount() + 1) : 1;
					int resUpdateLoginCnt = memberRepository.updateMemberLoginCount(tryLoginMember.getId(), updateCountValue);
				}

				// 로그인 실패 이력 남기기
				Log failLog = Log.builder()
						.logIp(clientIp)
						.workerName("")
						.logContent("[로그인 실패]")
						.logType("l")
						.refTable("member")
						.refTableId(tryLoginMember.getId())
						.createDatetime(LocalDateTime.now())
						.createMemberId(tryLoginMember.getId())
						.build();
				logRepository.save(failLog);

			} else {
				log.debug("로그인 실패 - 존재하지 않는 아이디: {}", loginId);
			}
		}

		// 인증 실패 → 401 Unauthorized
		ResMessage<Object> resMessage = new ResMessage<>(resCode, resMsg, null);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json;charset=utf-8");
		response.getWriter().write(objectMapper.writeValueAsString(resMessage));
	}
}
