package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@Log4j2
public class RootController {
	
	// 루트 경로로 바로 접근하는 경우 로그인 페이지로 리다이렉트 (index.html 노출 방지)
	@GetMapping(value = "/")
	public String root() {
		return "redirect:/member/login";
	}


}
