package com.bada.cali.api;

import com.bada.cali.service.AgentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/api/agent")
@RestController("ApiAgentController")
@Log4j2
@RequiredArgsConstructor
public class AgentController {
	
	private final AgentServiceImpl agentService;
	
	@PostMapping(value = "/chkAgentInfo")
	public ResponseEntity<Map<String, String>> getAgentInfoByName(@RequestBody Map<String, String> agentInfo) {
		log.info("agentInfo: {}", agentInfo);
		log.info("agentName: {}", agentInfo.get("agentName"));
		
		return ResponseEntity.ok(agentService.chkAgentInfo(agentInfo.get("agentName")));
		
	}
	
}
