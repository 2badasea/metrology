package com.bada.cali.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Log4j2
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)    // 기본값(true). 애너테이션을 이용한 권한체크(NOTATION 1번)
public class CustomSecurityConfig {
	
	// 로그인 성공/실패에 따른 훅(hook) 주입
	private final AuthenticationSuccessHandler LoginSuccessHandler;
	private final AuthenticationFailureHandler LoginFailureHandler;
	// 자동로그인 기능을 위한 의존성 주입
	private final DataSource dataSource;
	
	// filterChain 메서드 호출 시, 전역에서 로그인 검증을 하지 않고, 원하는 URL의 자원을 반환
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
		log.info("================= configure ===============");
		
		http
				// csrf 토큰확인 비활성화 처리 (NOTATION 3번)
				.csrf(AbstractHttpConfigurer::disable)
				// 접근 허용 경로 설정
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/login"
								, "/error"
								, "/member/login**"
								, "/member/memberJoin"
								, "/apiMember/**").permitAll()    // 해당 경로 접근 허용
						.anyRequest().authenticated()    // 그외 요청에 대해선 인증된 사용자만 허용
				)
				// 로그인폼 처리
				.formLogin(form -> form
						// .loginPage("/login") 스프링시큐리티에서 제공하는 기본 로그인 페이지
						.loginPage("/member/login").permitAll()    // 시큐리티 제공 기본 로그인 UI가 아닌 커스텀 페이지 경로 지정
						.loginProcessingUrl("/apiMember/login")    // 로그인 처리 URL 매핑 (컨트롤러를 타지 않고 Security Filter이 가로채(intercept) 직접 처리
						.usernameParameter("username")            // 아이디 입력값
						.passwordParameter("password")            // 비밀번호 입력값
						.successHandler(LoginSuccessHandler)    // 로그인 성공 훅
						.failureHandler(LoginFailureHandler)    // 로그인 실패 훅
				);
		
		// 자동로그인 설정
		http
				.rememberMe(rememberMe -> rememberMe
						.key("12345678")            // 토큰 생성 시 서명용 비밀 키(쿠키 값 조작여부 검증). 실무에선 UUID 같은 걸 환경변수/설정파일로 빼서 사용
						.tokenRepository(persistentTokenRepository())    // 자동로그인 토큰 저장 위치 지정(쿠키엔 series+token 저장, 서버 DB에서 매칭 확인)
						.userDetailsService(userDetailsService)            // 자동로그인 시, 쿠키/토큰에 담긴 사용자 정보로 실제 유저를 로딩하는 서비스
						.tokenValiditySeconds(60 * 60 * 24 * 30)    // 쿠키의 유효기간 설정(30일)
				);
		
		// 로그아웃 처리
		http
				.logout(logout -> logout
						.logoutUrl("/logout")        // 로그아웃 호출 URL
						.logoutSuccessHandler((request, response, authentication) -> {
							// 1) JSON 응답(원하면 redirect URL 같은 형태로 내려도 됨)
							response.setContentType("application/json;charset=UTF-8");
							response.getWriter().write("{\"ok\":true,\"redirect\":\"/member/login?logout\"}");
						})
						.invalidateHttpSession(true)                                    // 세션 무효화
						.clearAuthentication(true)                                        // SecurityContext 비우기
						.deleteCookies("JSESSIONID", "remember-me")    // 쿠키 삭제(안전하게 한번 더)
				);
		// 예외처리 (아래 @Bean 등록)
		http
				.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthenticatedEntryPoint()));
		
		return http.build();
	}
	
	// 자동로그인 기능을 위한 등록
	@Bean
	public PersistentTokenRepository persistentTokenRepository() {
		JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
		repository.setDataSource(dataSource);
		return repository;
	}
	
	// css, js, images 등에는 필터가 적용될 필요가 없으므로, 정적 리소스에 대해선 보안 필터를 적용하지 않도록 명시
	// filterChain()과 역할 중복. web.ignoring()은 보안 필터 자체를 아예 태우지 않음. 즉, filterChain 자체를 스킵
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		log.info("===============web webSecurityCustomizer====================");
		return (web) -> web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations());
	}
	
	// 암호화 알고리즘 빈 등록
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	// 허용되지 않은 URL 접근 시에 대한 예외처리
	@Bean
	public AuthenticationEntryPoint unauthenticatedEntryPoint() {
		return (request, response, authException) -> {
			// 여기서 “로그인이 필요한 상황”이라고 판단된 경우 login 페이지로 보냄
			String redirectUrl = "/member/login?required=-1";
			response.sendRedirect(redirectUrl);
		};
	}
	
	
}
