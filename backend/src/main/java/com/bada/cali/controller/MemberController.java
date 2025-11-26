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
import org.springframework.web.bind.annotation.RequestMethod;


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
	// NOTE 모달 페이지를 호출하는 경우 기본적으로 Post로 받는다. 모달, 일반페이지 모두 사용 시 RequestMapping을 이용하여 get과 post 모두 허용 가능
	@RequestMapping(value = "/memberJoin", method = {RequestMethod.POST, RequestMethod.GET})
	public String memberJoin(Model model) {
		log.info("memberJoin get................");
		return "member/memberJoin";
	}
	
}