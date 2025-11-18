package com.bada.cali.controller;

import jakarta.servlet.http.HttpServletRequest;
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
	public String login(Model model) {
		log.info("login get................");
		model.addAttribute("title", "로그인");
		return "member/login";
	}
	
	// 회원가입 [모달]
	@PostMapping("/memberJoin")
	public String memberJoin(Model model) {
		log.info("memberJoin get................");
		model.addAttribute("title", "회원가입");	// title 의미없음.
		return "member/memberJoin";
	}
	
}