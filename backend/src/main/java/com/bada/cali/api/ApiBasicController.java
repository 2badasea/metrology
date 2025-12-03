package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.AgentManagerRepository;
import com.bada.cali.repository.MemberRepository;
import com.bada.cali.security.CustomUserDetailService;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.AgentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.List;

@RestController        // @Controller + @ResponseBody의 조합으로, 자동으로 응답 데이터를 JSON 형태로 직렬화해서 리턴한다.
@RequestMapping("/api/basic")
@Log4j2
@RequiredArgsConstructor
public class ApiBasicController {
	
	private final AgentServiceImpl agentService;
	private final AgentManagerRepository agentManagerRepository;
	
	// 업체관리 리스트 가져오기 (토스트 그리드)
	@GetMapping(value = "/getAgentList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<AgentDTO.AgentRowData>>> getAgentList(@ModelAttribute AgentDTO.GetListReq req) {
		
		TuiGridDTO.ResData<AgentDTO.AgentRowData> resGridData = agentService.getAgentList(req);
		
		// data.api.readData 규약에 맞게 DTO로 감싸기
		TuiGridDTO.Res<TuiGridDTO.ResData<AgentDTO.AgentRowData>> body =
				new TuiGridDTO.Res<>(true, resGridData);
		
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
	
	// 업체그룹관리 그룹관리 정보가져오기
	@PostMapping(value = "/getGroupName")
	public ResponseEntity<ResMessage<List<String>>> getGroupName() {
		List<String> resGroupNames = agentService.getGroupName();
		
		// 존재하면 응답코드 1
		int resCode = resGroupNames.isEmpty() ? -1 : 1;
		return ResponseEntity.ok(new ResMessage<>(resCode, null, resGroupNames));
	}
	
	// 업체 그룹관리 수정
	@PostMapping(value = "/updateGroupName")
	public ResponseEntity<ResMessage<Object>> updateGroupName(
			@RequestBody AgentDTO.UpdateGroupNameReq req,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		log.info("=========== updateGroupName");
		// 업데이트 실행
		int resUpdate = agentService.updateGroupName(req, user);
		// 성공 1 반환
		int resCode = resUpdate > 0 ? 1 : -1;
		return ResponseEntity.ok(new ResMessage<>(resCode, null, null));
	}
	
	// 특정 업체의 담당자 정보 가져오기
	@GetMapping(value = "/getAgentManagerList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData>>> getAgentManagerList(@ModelAttribute AgentManagerDTO.GetListReq req) {
		log.info("========== getAgentManagerList");
		
		// 업체관련 서비스계층에서 리스트 가져오도록 처리
		TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData> resGridData = agentService.getAgentManagerList(req);
		
		TuiGridDTO.Res<TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData>> body =
				new TuiGridDTO.Res<>(true, resGridData);
		
		return ResponseEntity.ok(body);
	}
	
	@PostMapping(value = "/saveAgent")
	public ResponseEntity<ResMessage<Object>> saveAgent(
			@RequestPart("saveAgentDataReq") AgentDTO.SaveAgentDataReq saveAgentDataReq,
			@RequestPart(value = "files", required = false) List<MultipartFile> files,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		
		int resSaveAgent = agentService.saveAgent(saveAgentDataReq, files, user);
		
		
		
		return null;
	}
	
	
	
	
}
