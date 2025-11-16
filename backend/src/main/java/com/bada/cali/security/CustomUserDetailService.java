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
		Member loginMember = memberRepository.findByLoginId(username).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
		
		// 로그인 시도 5회 이상 && 마지막 로그인 시도 일시가 10분도 안 된 상태라면 실패로 던지기
		LocalDateTime now = LocalDateTime.now();
		if (loginMember.getLoginCount() >= 5 && loginMember.getLastLoginFailDatetime() != null && loginMember.getLastLoginFailDatetime().plusMinutes(10).isAfter(now)) {
			throw new LockedException("10분 뒤에 로그인이 가능합니다.");
		}
		// 계정의 가입 승인이 이루어지지 않은 경우 y: 가능(승인완료), n: 불가(미승인/차단)
		if (loginMember.getIsActive() != Member.YnType.valueOf("y")) {
			throw new LockedException("10분 뒤에 로그인이 가능합니다.");
		}
		
		UserDetails userDetails = User.withUsername(loginMember.getLoginId())
				.password(loginMember.getPwd())                        // DB의 해쉬값 그대로
				.authorities("ROLE_USER")                        // 최소 권한이라도 넣어두기(필요시 실제 권한 매핑)
				.build();
		
		return userDetails;
	}
}
