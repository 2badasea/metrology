package com.bada.cali.security;

import com.bada.cali.common.ResMessage;
import com.bada.cali.entity.Log;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Log4j2
public class LoginFailureHandler implements AuthenticationFailureHandler {
	
	private static final String FAIL_MSG = "아이디 또는 비밀번호가 일치하지 않습니다.";
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
		log.info("Login fail hook called");
		
		String resMsg = FAIL_MSG;
		int resCode = -1;
		
		String loginId = request.getParameter("username");
		
		// 계정이 차단되어 있거나 로그인 시도 10분 제한이 걸린 경우
		if (exception instanceof InternalAuthenticationServiceException && exception.getCause() instanceof LockedException lockedException) {
			resMsg = lockedException.getMessage();
		} else {
			Optional<Member> optLoginMember = memberRepository.findByLoginId(loginId, Member.YnType.y);
			if (optLoginMember.isPresent()) {
				log.info("==== 확인");
				Member tryLoginMember = optLoginMember.get();
				// 입력 아이디 존재 시, 로그인 시도 횟수 카운트 (현재 값이 4이하 일때만 업데이트)
				if (tryLoginMember.getLoginCount() <= 5) {
					int updateCountValue = (tryLoginMember.getLoginCount() <= 4) ? (tryLoginMember.getLoginCount() + 1) : 1;
					int resUpdateLoginCnt = memberRepository.updateMemberLoginCount(tryLoginMember.getId(), updateCountValue);
				}
				
				// 로그인 실패 이력 남기기
				Log failLog = Log.builder()
						.workerName("")
						.logContent("[로그인 실패] - 로그인 시도 횟수: " + (tryLoginMember.getLoginCount() + 1))
						.logType("l")
						.refTable("member")
						.refTableId(tryLoginMember.getId())
						.createDatetime(LocalDateTime.now())
						.createMemberId(tryLoginMember.getId())
						.build();
				Log resSaveLog = logRepository.save(failLog);
				
			} else {
				log.info("로그인 실패 훅 내부 오류");
			}
		}
		
		// 응답 메시지 생성
		ResMessage<Object> resMessage = new ResMessage<>(resCode, resMsg, null);
		response.setStatus(HttpStatus.OK.value());        // 500에러를 제외하고 잡을 수 있는 에러들에 대해선 정상흐름으로 상태값 리턴하기
		response.setContentType("application/json;charset=utf-8");
		response.getWriter().write(objectMapper.writeValueAsString(resMessage));
	}
}
