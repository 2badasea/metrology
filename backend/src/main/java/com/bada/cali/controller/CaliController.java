package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cali")
@Log4j2
public class CaliController {
	
	// 교정접수 페이지 이동
	@GetMapping(value = "/caliOrder")
	public String caliOrder(Model model) {
		log.info("============== cali_order");
		model.addAttribute("title", "교정접수");
		return "cali/caliOrder";
	}
}
