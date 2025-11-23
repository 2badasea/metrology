package com.bada.cali.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/basic")
@Log4j2
@RequiredArgsConstructor
public class BasicController {
	
	// 기본 페이지 리턴
	// TODO 추후 관리자와 일반 사용자의 로그인 성공 시, 경로 구분할 것
	@GetMapping("/home")
	public String home(Model model) {
		model.addAttribute("title", "홈");
		// NOTE 기본 설정(스프링부트)의 탐색경로: classpath:/templates/ + viewname + .html => /basic/home 을 명시할 경우, //basic/home이 된다.
		return "basic/home";
	}
	
	// 업체관리 페이지
	@GetMapping(value = "/agentList")
	public String agentList(Model model) {
		log.info("업체관리 페이지 이동");
		return "basic/agentList";
	}
}
