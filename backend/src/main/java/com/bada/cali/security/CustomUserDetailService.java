package com.bada.cali.security;

import com.bada.cali.entity.Member;
import com.bada.cali.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
	
	private final MemberRepository memberRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.info("loadUserByUsername username={}", username);
		
		
		// orElseThrow의 경우, 값이 있으면 Optional<T>에 명시한 T타입의 값(get)으로 반환하도록 동작
		// 시큐리티 필터 내부에서 UsernameNotFoundException를 던져도 실패 훅에선 BadCredentialException으로 받게 됨.(NOTATION 6번 확인)
		Member loginMember = memberRepository.findByLoginId(username, Member.YnType.y).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
		
		// 로그인 시도 5회 이상 && 마지막 로그인 시도 일시가 10분도 안 된 상태라면 실패로 던지기
		// LockedException을 던졌을 때, 실패 훅에서 해당 예외로 직접받을 수 없음 (NOTATION 6번 확인)
		LocalDateTime now = LocalDateTime.now();
		if (loginMember.getLoginCount() >= 5 && loginMember.getLastLoginFailDatetime() != null && loginMember.getLastLoginFailDatetime().plusMinutes(10).isAfter(now)) {
			throw new LockedException("10분 뒤에 로그인이 가능합니다.");
		}
		// 계정의 가입 승인이 이루어지지 않은 경우 y: 가능(승인완료), n: 불가(미승인/차단)
		if (loginMember.getIsActive() != Member.YnType.valueOf("y")) {
			throw new LockedException("관리자의 승인이 필요합니다.\n관리자에게 문의해주세요.");
		}
		
		UserDetails userDetails = User.withUsername(loginMember.getLoginId())
				.password(loginMember.getPwd())                        // DB의 해쉬값 그대로
				.authorities("ROLE_USER")                        // 최소 권한이라도 넣어두기(필요시 실제 권한 매핑)
				.build();
		
		return userDetails;
	}
}
