package com.bada.cali.api;

import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.MemberRepository;
import com.bada.cali.service.AgentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController        // @Controller + @ResponseBody의 조합으로, 자동으로 응답 데이터를 JSON 형태로 직렬화해서 리턴한다.
@RequestMapping("/apiBasic")
@Log4j2
@RequiredArgsConstructor
public class apiBasicController {
	
	private final AgentServiceImpl agentService;
	
	// 업체관리 리스트 가져오기 (토스트 그리드)
	@GetMapping(value = "/getAgentList")
	public ResponseEntity<TuiGridDTO.ResTuiGrid<TuiGridDTO.Response<AgentDTO.AgentRowData>>> getAgentList(@ModelAttribute AgentDTO.GetListReq req) {
		log.info("==========getAgentList==========");
		
		TuiGridDTO.Response<AgentDTO.AgentRowData> resGridData = agentService.getAgentList(req);
		
		// data.api.readData 규약에 맞게 DTO로 감싸기
		TuiGridDTO.ResTuiGrid<TuiGridDTO.Response<AgentDTO.AgentRowData>> body =
				new TuiGridDTO.ResTuiGrid<>(true, resGridData);
		
		return ResponseEntity.ok(body);
	}
	
	// 업체 정보 가져오기(개별)
	@PostMapping(value = "/getAgentInfo")
	public ResponseEntity<AgentDTO.AgentRowData> getAgentInfo(@RequestParam Integer id) {
		log.info("==========getAgentInfo");
		log.info("==========id: {}", id);
		
		AgentDTO.AgentRowData resAgentData = agentService.getAgentInfo(id);
		return ResponseEntity.ok(resAgentData);
	}
	
	
}
