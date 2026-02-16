package com.bada.cali.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@EnableConfigurationProperties(RememberMeProperties.class)
public class CustomSecurityConfig {

	private final AuthenticationSuccessHandler LoginSuccessHandler;        // 로그인 성공 훅
	private final AuthenticationFailureHandler LoginFailureHandler;        // 로그인 실패 훅
	private final DataSource dataSource;        // 자동로그인 기능을 위한 의존성 주입
	private final RememberMeProperties rememberMeProperties;              // Remember-Me 설정
	
	// filterChain 메서드 호출 시, 전역에서 로그인 검증을 하지 않고, 원하는 URL의 자원을 반환
	// NOTE 시큐리티 필터체인(filterChain) 내부에서 던지는 예외들에 대해선 로그인 성공/실패 훅 내부로 전달됨(훅 내부에서 체크 가능)
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
		log.debug("================= configure ===============");

		http
				.csrf(AbstractHttpConfigurer::disable)                    // csrf 토큰확인 비활성화 처리 (NOTATION 3번)

				// 접근 허용 경로 설정
				.authorizeHttpRequests(auth -> auth
						// DELETE는 무조건 ADMIN만 허용
						.requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
						// 정적 리소스는 아래 WebSecurityCustomizer에서 처리
						.requestMatchers(
								"/error"
								, "/member/login"            // 쿼리스트링(?error, ?logout 등)은 경로 매칭에 포함되지 않으므로 정확한 경로만 명시
								, "/member/memberJoin"
								, "/guide/**"
								, "/api/member/**"
						).permitAll()
						.anyRequest().authenticated()                // 그외 요청에 대해선 인증된 사용자만 허용
				)

				// 로그인폼 처리
				.formLogin(form -> form
						.loginPage("/member/login").permitAll()    // 커스텀 로그인 페이지 경로 지정
						.loginProcessingUrl("/api/member/login")    // 로그인 처리 URL (Security Filter가 가로채서 직접 처리)
						.usernameParameter("username")
						.passwordParameter("password")
						.successHandler(LoginSuccessHandler)
						.failureHandler(LoginFailureHandler)
				)

				// 자동로그인 설정
				.rememberMe(rememberMe -> rememberMe
						.key(rememberMeProperties.key())                // application.properties에서 주입받은 서명용 키
						.tokenRepository(persistentTokenRepository())    // 자동로그인 토큰 저장 위치 (DB)
						.userDetailsService(userDetailsService)            // 쿠키/토큰으로 실제 유저를 로딩하는 서비스
						.tokenValiditySeconds(60 * 60 * 24 * 30)        // 쿠키 유효기간 (30일)
				)

				// 로그아웃 처리
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessHandler((request, response, authentication) -> {
							response.setContentType("application/json;charset=UTF-8");
							response.getWriter().write("{\"ok\":true,\"redirect\":\"/member/login?logout\"}");
						})
						.invalidateHttpSession(true)                                // 세션 무효화
						.clearAuthentication(true)                                    // SecurityContext 비우기
						.deleteCookies("JSESSIONID", "remember-me")    // 쿠키 삭제
				)

				// 인증이 필요한 URL에 미인증 접근 시 → AuthenticationEntryPoint로 위임하여 로그인 페이지로 리다이렉트
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
	
	// 정적 리소스(css, js, images 등)는 보안 필터 자체를 스킵하여 불필요한 필터 처리 방지 (성능 이점)
	// web.ignoring(): 필터 체인 자체를 태우지 않음 (SecurityContext 없음)
	// permitAll(): 필터 체인은 통과하되 인증 없이 접근 허용 (SecurityContext 존재)
	// → 정적 리소스는 ignoring, URL 경로 접근 제어는 permitAll로 역할을 분리
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
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
			String redirectUrl = "/member/login" + ("/".equals(request.getRequestURI()) ? "" : "?required=-1");
			response.sendRedirect(redirectUrl);
		};
	}
	
	
}
