package com.bada.cali.security;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Log4j2
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
	
	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;        // Java 객체 -> JSON 문자열로 변환(직렬화)해주는 메서드 지원 (Jackson 라이브러리에서 제공)
	private final LogRepository logRepository;
	
	/**
	 * 로그인 성공에 대한 훅
	 * FIX 로그인 성공에 대한 훅도 service 층에서 처리할 수 있도록 수정 후, 서비스 계층에 @Transactional 명시할 것 (현재 repository 계층에 명시 중)
	 * @param request
	 * @param response
	 * @param authentication
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		log.info("Login success hook called");
		
		String resMsg = "로그인 성공";
		int resCode = 2;
		
		String loginId = authentication.getName();
		// loginId를 바탕으로 유저 정보를 가져온다.
		Member loginMember = memberRepository.findByLoginId(loginId).orElseThrow(() -> new RuntimeException("로그인 처리 중 오류 발생(called by LoginSuccessHandler"));
		
		LocalDateTime now = LocalDateTime.now();
		
		// 이력남기기 (TODO 'logip'를 남기는 것에 대해선 추후 로그를 남기는 것 자체를 service로 분리해서 처리할 것)
		Log logEntity = Log.builder()
				.workerName(loginMember.getName())
				.logContent("[로그인 성공] - " + loginMember.getName() + " 님이 로그인 하셨습니다.")
				.logType("l")
				.refTable("member")
				.refTableId(loginMember.getId())
				.createDatetime(now)
				.createMemberId(loginMember.getId())
				.build();
		
		// 이력 저장
		Log resSaveLog = logRepository.save(logEntity);
		
		// 로그인 시도 count 초기화
		if (loginMember.getLoginCount() > 0) {
			// JpaRepository는 메서드 이름만 보고 쿼리를 자동으로 생성 및 수행하지만, 업데이트/삭제는 그렇게 동작 X
			int resUpdateLoginCount = memberRepository.updateMemberLoginCount(loginMember.getId(), 0);
		}
		
		
		
		
		
	}
}
