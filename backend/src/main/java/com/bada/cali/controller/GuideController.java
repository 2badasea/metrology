package com.bada.cali.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/guide")
@Log4j2
@RequiredArgsConstructor
public class GuideController {
	
	
	// 테스터 가이드 안내창을 띄운다.
	@PostMapping("/testerIntro")
	public String testerIntro() {
		return "guide/testerIntro";
	}
	
}
