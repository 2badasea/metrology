package com.bada.cali.controller;

import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.service.AgentServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/agent")
@Log4j2
@AllArgsConstructor
public class AgentController {
	private final AgentServiceImpl agentService;
	
	// 업체 조회 [모달]
	@PostMapping(value = "/searchAgentModify")
	public String caliOrderModify(Model model,
								  @RequestParam(value = "agentFlag", required = false) Integer agentFlag,
								  @RequestParam(value = "agentName", required = false) String agentName
	) {
		model.addAttribute("agentFlag", agentFlag);
		model.addAttribute("agentName", agentName);
		return "agent/searchAgentModify";
	}
	
	// 업체 담당자 조회 [모달]
	@PostMapping(value = "/searchAgentManager")
	public String searchAgentManager(Model model, @RequestParam Long agentId) {
		List<AgentManagerDTO.AgentManagerRowData> managerList = agentService.getManagerList(agentId);
		model.addAttribute("managerList", managerList);
		
		return "agent/searchAgentManager";
	}
	
}
