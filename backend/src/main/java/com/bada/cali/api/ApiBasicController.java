package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.MemberRepository;
import com.bada.cali.security.CustomUserDetailService;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.AgentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController        // @Controller + @ResponseBody의 조합으로, 자동으로 응답 데이터를 JSON 형태로 직렬화해서 리턴한다.
@RequestMapping("/apiBasic")
@Log4j2
@RequiredArgsConstructor
public class ApiBasicController {
	
	private final AgentServiceImpl agentService;
	
	// 업체관리 리스트 가져오기 (토스트 그리드)
	@GetMapping(value = "/getAgentList")
	public ResponseEntity<TuiGridDTO.ResTuiGrid<TuiGridDTO.Response<AgentDTO.AgentRowData>>> getAgentList(@ModelAttribute AgentDTO.GetListReq req) {
		
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
	
	// 업체 삭제 요청
	@PostMapping("/deleteAgent")
	public ResponseEntity<ResMessage<List<String>>> deleteAgent(
			@RequestBody AgentDTO.DelAgentReq delAgentReq,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		log.info("=========== deleteAgent");
		List<String> delAgentNames = agentService.deleteAgent(delAgentReq, user);
		
		ResMessage<List<String>> resMessage = new ResMessage<>();
		resMessage.setCode(1);
		resMessage.setMsg("삭제되었습니다.");
		resMessage.setData(delAgentNames);
		
		return ResponseEntity.ok(resMessage);
	}
	
	
}
