package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/agent")
@Log4j2
public class AgentController {
	
	// 업체 조회 [모달]
	@PostMapping(value = "/searchAgentModify")
	public String caliOrderModify(Model model,
								  @RequestParam(value = "agentFlag", required = false) Integer agentFlag,
								  @RequestParam(value = "agentName", required = false) String agentName
	) {
		log.info("agentFlag: {}", agentFlag);
		log.info("agentName: {}", agentName);
		model.addAttribute("agentFlag", agentFlag);
		model.addAttribute("agentName", agentName);
		return "agent/searchAgentModify";
	}
	
}
