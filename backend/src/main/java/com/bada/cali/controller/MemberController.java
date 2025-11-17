package com.bada.cali.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/member")
@Log4j2
@RequiredArgsConstructor
public class MemberController {
	
	// 커스텀 로그인 페이지
	@GetMapping("/login")
	public String login(String error, String logout) {
		log.info("login get................");
		return "member/login";
	}
	
	// 테스트 페이지 (애너테이션을 이용한 권한체크를 위함)
	@PreAuthorize("hasRole('USER')")    // 경로를 리턴하기 전에 권한체크 -> false 리턴 시 로그인 페이지로 리다이렉트된다.
	@GetMapping("/logintest")
	public String logintest(Model model) {
		return "member/logintest";
	}
	
	// 회원가입 페이지 이동
	@PostMapping("/memberJoin")
	public String memberJoin(Model model) {
		log.info("memberJoin get................");
		return "member/memberJoin";
	}
	
	
}