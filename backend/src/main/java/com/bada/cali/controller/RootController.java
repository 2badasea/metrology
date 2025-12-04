package com.bada.cali.controller;

import com.bada.cali.security.CustomUserDetails;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@Log4j2
public class RootController {
	
	// 루트 경로로 바로 접근하는 경우 로그인 페이지로 리다이렉트 (index.html 노출 방지)
	@GetMapping(value = "/")
	public String root(@AuthenticationPrincipal CustomUserDetails user) {
		
		// 세션이 이미 존재하는 경우엔 기본 페이지로 리턴
		if (user != null) {
			return "redirect:/basic/home";
		}
		
		// 비인증 상태인 경우
		return "redirect:/member/login";
	}


}
