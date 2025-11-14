package com.bada.cali.security;

import com.bada.cali.entity.Member;
import com.bada.cali.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
	
	private final MemberRepository memberRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.info("loadUserByUsername username={}", username);
		
		// orElseThrow의 경우, 값이 있으면 Optional<T>에 명시한 T타입의 값(get)으로 반환하도록 동작
		Member member = memberRepository.findByLoginId(username).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
		log.info("member={}", member);
		
		UserDetails userDetails = User.withUsername(member.getLoginId())
				.password(member.getPwd())						// DB의 해쉬값 그대로
				.accountLocked(member.getIsActive() != 1)		// 0 = 로그인 불가, 1 = 가능
				.authorities("ROLE_USER")						// 최소 권한이라도 넣어두기(필요시 실제 권한 매핑)
				.build();
		
		return userDetails;
	}
}
