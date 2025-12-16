package com.bada.cali.controller;

import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.repository.AgentManagerRepository;
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
	private final AgentManagerRepository agentManagerRepository;
	
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
	
	// 업체 담당자 조회 [모달]
	@PostMapping(value = "/searchAgentManager")
	public String searchAgentManager(Model model, @RequestParam Long agentId) {
		log.info("agentId: {}", agentId);
		
		// 업체담당자의 정보를 바로 가져온다(네이티브 쿼리 사용)
		// @Query 애너테이션을 이용하여 DTO반환을 하기 위해서는 select절에서 생성자를 통해 객체 반환
		List<AgentManagerDTO.AgentManagerRowData> managerList = agentManagerRepository.getManagerListOrderByMainYn(agentId);
		model.addAttribute("managerList", managerList);
		
		return "agent/searchAgentManager";
	}
	
}
