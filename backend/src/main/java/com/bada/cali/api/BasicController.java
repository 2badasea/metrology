package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.dto.*;
import com.bada.cali.repository.AgentManagerRepository;
import com.bada.cali.repository.projection.DepartmentListPr;
import com.bada.cali.repository.projection.ItemCodeList;
import com.bada.cali.repository.projection.MemberLevelListPr;
import com.bada.cali.repository.projection.OrderDetailsList;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.AgentServiceImpl;
import com.bada.cali.service.BasicServiceImpl;
import com.bada.cali.service.ItemCodeServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Tag(name = "기본정보", description = "업체·업체담당자·분류코드·부서·직급 기본 정보 관리 API")
@RestController ("ApiBasicController")       // @Controller + @ResponseBody의 조합으로, 자동으로 응답 데이터를 JSON 형태로 직렬화해서 리턴한다.
@RequestMapping("/api/basic")
@Log4j2
@RequiredArgsConstructor
public class BasicController {
	
	private final AgentServiceImpl agentService;
	private final ItemCodeServiceImpl itemCodeService;
	private final BasicServiceImpl basicService;
	
	// 업체관리 리스트 가져오기 (토스트 그리드)
	// NOTE 그리드 api형식에 맞춰서 데이터를 받기 때문에, JSON.stringify() 처리를 하지 않았기에 @ModelAttribute로 받음.
	@Operation(summary = "업체 목록 조회", description = "검색 조건으로 업체 목록을 페이지네이션하여 조회. Toast UI Grid 규약에 맞는 형식으로 응답")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getAgentList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<AgentDTO.AgentRowData>>> getAgentList(@ModelAttribute AgentDTO.GetListReq req) {
		TuiGridDTO.ResData<AgentDTO.AgentRowData> resGridData = agentService.getAgentList(req);
		// data.api.readData 규약에 맞게 DTO로 감싸기
		TuiGridDTO.Res<TuiGridDTO.ResData<AgentDTO.AgentRowData>> body =
				new TuiGridDTO.Res<>(true, resGridData);
		
		return ResponseEntity.ok(body);
	}
	
	// 업체 정보 가져오기(개별)
	@Operation(summary = "업체 단건 조회", description = "업체 고유 id로 업체 상세 정보 및 첨부파일 수 조회. 삭제된 업체(is_visible=n)는 조회되지 않음")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "업체 정보 없음",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PostMapping(value = "/getAgentInfo")
	public ResponseEntity<AgentDTO.AgentRowData> getAgentInfo(@RequestParam Long id) {
		
		AgentDTO.AgentRowData resAgentData = agentService.getAgentInfo(id);
		return ResponseEntity.ok(resAgentData);
	}
	
	// 업체 삭제 요청
	@Operation(summary = "업체 삭제", description = "선택된 업체를 소프트 삭제. 하위 담당자·계정도 함께 삭제 처리")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "400", description = "요청 형식 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@DeleteMapping("/deleteAgent")
	public ResponseEntity<ResMessage<List<String>>> deleteAgent(
			@RequestBody AgentDTO.DelAgentReq delAgentReq,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		List<String> delAgentNames = agentService.deleteAgent(delAgentReq, user);
		
		ResMessage<List<String>> resMessage = new ResMessage<>();
		resMessage.setCode(1);
		resMessage.setMsg("삭제되었습니다.");
		resMessage.setData(delAgentNames);
		
		return ResponseEntity.ok(resMessage);
	}
	
	// 업체그룹관리 그룹관리 정보가져오기
	@Operation(summary = "업체 그룹명 목록 조회", description = "등록된 업체 그룹명 목록 조회. 결과 없으면 code=-1 반환")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getGroupName")
	public ResponseEntity<ResMessage<List<String>>> getGroupName() {
		List<String> resGroupNames = agentService.getGroupName();
		
		// 존재하면 응답코드 1
		int resCode = resGroupNames.isEmpty() ? -1 : 1;
		return ResponseEntity.ok(new ResMessage<>(resCode, null, resGroupNames));
	}
	
	// 업체 그룹관리 수정
	@Operation(summary = "업체 그룹명 수정", description = "선택된 업체들의 그룹명 일괄 수정")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "400", description = "요청 형식 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
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
	@Operation(summary = "업체 담당자 목록 조회", description = "특정 업체의 담당자 목록 조회. Toast UI Grid 규약에 맞는 형식으로 응답")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getAgentManagerList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData>>> getAgentManagerList(@ModelAttribute AgentManagerDTO.GetListReq req) {
		log.info("========== getAgentManagerList");
		
		// 업체관련 서비스계층에서 리스트 가져오도록 처리
		TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData> resGridData = agentService.getAgentManagerList(req);
		
		TuiGridDTO.Res<TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData>> body =
				new TuiGridDTO.Res<>(true, resGridData);
		
		return ResponseEntity.ok(body);
	}
	
	// 업체 등록
	@Operation(summary = "업체 등록", description = "신규 업체 정보 등록. 신청업체(agentFlag=1)이고 사업자번호가 있으면 업체 계정(member)도 자동 생성")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "등록 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PostMapping(value = "/agents")
	public ResponseEntity<ResMessage<Object>> createAgent(
			@RequestPart("saveAgentDataReq") AgentDTO.SaveAgentDataReq saveAgentDataReq,
			@RequestPart(value = "files", required = false) List<MultipartFile> files,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		int res = agentService.saveAgent(saveAgentDataReq, files, user);
		return ResponseEntity.status(201).body(new ResMessage<>(res, null, null));
	}

	// 업체 수정
	@Operation(summary = "업체 수정", description = "기존 업체 정보 수정. 업체 또는 담당자 id가 존재하지 않으면 404")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "404", description = "업체 또는 담당자 정보 없음",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PatchMapping(value = "/agents/{id}")
	public ResponseEntity<ResMessage<Object>> updateAgent(
			@Parameter(description = "업체 고유 id", required = true, example = "1")
			@PathVariable Long id,
			@RequestPart("saveAgentDataReq") AgentDTO.SaveAgentDataReq saveAgentDataReq,
			@RequestPart(value = "files", required = false) List<MultipartFile> files,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		int res = agentService.saveAgent(saveAgentDataReq, files, user);
		return ResponseEntity.ok(new ResMessage<>(res, null, null));
	}
	
	// 분류코드 정보 가져오기 (토스트리스트)
	@Operation(summary = "분류코드 목록 조회", description = "코드레벨·상위코드·키워드로 분류코드 목록을 페이지네이션하여 조회. Toast UI Grid 규약에 맞는 형식으로 응답")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getItemCodeList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<ItemCodeList>>> getItemCodeList(@ModelAttribute ItemCodeDTO.ItemCodeListReq req) {
		
		
		// 리스트 데이터 가져오기 (인터페이스 프로젝션 형태로 가져옴 )
		TuiGridDTO.ResData<ItemCodeList> itemCodeGridData = itemCodeService.getItemCodeList(req);
		
		TuiGridDTO.Res<TuiGridDTO.ResData<ItemCodeList>> body = new TuiGridDTO.Res<>(true, itemCodeGridData);
		
		return ResponseEntity.ok(body);
	}
	
	// 분류코드 등록
	@Operation(summary = "분류코드 등록", description = "분류코드 일괄 등록. 분류코드 중복 시 code=-1 반환")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "등록 성공"),
			@ApiResponse(responseCode = "400", description = "요청 형식 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PostMapping("/itemCodes")
	public ResponseEntity<ResMessage<Object>> createItemCodes(
			@RequestBody List<ItemCodeDTO.ItemCodeData> req,
			@AuthenticationPrincipal CustomUserDetails user) {
		ResMessage<Object> resMessage = itemCodeService.createItemCodes(req, user);
		return ResponseEntity.status(201).body(resMessage);
	}

	// 분류코드 수정
	@Operation(summary = "분류코드 수정", description = "분류코드 일괄 수정. 분류코드 중복 시 code=-1 반환. 해당 id가 존재하지 않으면 404")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "400", description = "요청 형식 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "404", description = "수정 대상 분류코드 없음",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PatchMapping("/itemCodes")
	public ResponseEntity<ResMessage<Object>> updateItemCodes(
			@RequestBody List<ItemCodeDTO.ItemCodeData> req,
			@AuthenticationPrincipal CustomUserDetails user) {
		ResMessage<Object> resMessage = itemCodeService.updateItemCodes(req, user);
		return ResponseEntity.ok(resMessage);
	}
	
	// 삭제대상 분류코드의 데이터를 검증한다.
	@Operation(summary = "분류코드 삭제 전 검증", description = "삭제 대상 분류코드의 하위 코드 존재 여부 및 성적서 참조 여부 검증. 삭제 불가 시 code=-1 반환")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "검증 완료"),
			@ApiResponse(responseCode = "400", description = "요청 형식 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PostMapping(value = "/deleteItemCodeCheck")
	public ResponseEntity<ResMessage<Map<String, String>>> deleteItemCodeCheck(
			@RequestBody ItemCodeDTO.DeleteCheckReq req
	) {
		
		ResMessage<Map<String, String>> resMessage = itemCodeService.deleteItemCodeCheck(req);
		return ResponseEntity.ok(resMessage);
	}
	
	// 분류코드 최종 삭제처리
	@Operation(summary = "분류코드 삭제", description = "검증 완료된 분류코드 최종 소프트 삭제. 하위 분류코드도 함께 삭제 처리")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "400", description = "요청 형식 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@DeleteMapping(value = "/deleteItemCode")
	public ResponseEntity<ResMessage<Object>> deleteItemCode(
			@RequestBody ItemCodeDTO.DeleteCheckReq req,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		
		ResMessage<Object> resMessage = itemCodeService.deleteItemCode(req, user);
		
		return ResponseEntity.ok(resMessage);
	}
	
	@Operation(summary = "코드레벨별 분류코드 목록 조회", description = "지정한 코드레벨의 분류코드 전체 목록 조회")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping("/getItemCodeSet")
	public ResponseEntity<ResMessage<List<ItemCodeList>>> getItemCodeSet(
			@RequestParam CodeLevel codeLevel
	) {
		
		ResMessage<List<ItemCodeList>> resMessage = itemCodeService.getItemCodeSet(codeLevel);
		
		return ResponseEntity.ok(resMessage);
	}
	
	@Operation(summary = "분류코드 전체 정보 조회", description = "직원 등록/수정 화면에서 사용하는 중분류·소분류 코드 전체 정보를 구조화하여 조회")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getItemCodeInfos")
	public ResponseEntity<ResMessage<ItemCodeDTO.ItemCodeInfosRes>> getItemCodeSet() {
		ResMessage<ItemCodeDTO.ItemCodeInfosRes> resMessage = itemCodeService.getItemCodeInfos();
		
		return ResponseEntity.ok(resMessage);
	}
	
	// 부서관리 리스트 가져오기
	@Operation(summary = "부서 목록 조회", description = "사용 중인 부서 목록 조회. Toast UI Grid 규약에 맞는 형식으로 응답")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getDepartmentList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<DepartmentListPr>>> getDepartmentList(
	) {
		
		// pagination, contents
		TuiGridDTO.ResData<DepartmentListPr> gridData = basicService.getDepartmentList();
		// boolean, data
		TuiGridDTO.Res<TuiGridDTO.ResData<DepartmentListPr>> body = new TuiGridDTO.Res<>(true, gridData);
		return ResponseEntity.ok(body);
	}
	
	// 직급관리 리스트 가져오기
	@Operation(summary = "직급 목록 조회", description = "사용 중인 직급 목록 조회. Toast UI Grid 규약에 맞는 형식으로 응답")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getMemberLevelList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<MemberLevelListPr>>> getMemberLevelList(
	) {
		
		// pagination, contents
		TuiGridDTO.ResData<MemberLevelListPr> gridData = basicService.getMemberLevelList();
		// boolean, data
		TuiGridDTO.Res<TuiGridDTO.ResData<MemberLevelListPr>> body = new TuiGridDTO.Res<>(true, gridData);
		return ResponseEntity.ok(body);
	}
	
	// 부서/직급관리 저장
	@Operation(summary = "부서/직급 저장", description = "부서·직급 정보 일괄 등록/수정/삭제. 수정 시 해당 id가 존재하지 않으면 404")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "저장 성공"),
			@ApiResponse(responseCode = "400", description = "요청 형식 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "404", description = "수정 대상 부서 또는 직급 없음",
					content = @Content(schema = @Schema(implementation = ResMessage.class))),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@PostMapping(value = "/saveBasicInfo")
	public ResponseEntity<ResMessage<?>> saveBasicInfo(
			@RequestBody BasicDTO.BasicSaveDataSet req,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		ResMessage<?> resMessage = basicService.saveBasicInfo(req, user);
		return ResponseEntity.ok(resMessage);
	}
	
	// 부서관리, 직급관리, 사용중인 중분류코드 정보를 가져온다 (직원등록/수정 세팅)
	@Operation(summary = "직원 등록/수정용 기본 옵션 조회", description = "직원 등록·수정 화면에서 사용하는 부서·직급·중분류코드 정보 일괄 조회")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "500", description = "서버 오류",
					content = @Content(schema = @Schema(implementation = ResMessage.class)))
	})
	@GetMapping(value = "/getBasicOptions")
	public ResponseEntity<ResMessage<BasicDTO.MemberModifySetting>> getBasicOptions() {
		ResMessage<BasicDTO.MemberModifySetting> resMessage = basicService.getBasicOptions();
		return ResponseEntity.ok(resMessage);
	}
	
}
