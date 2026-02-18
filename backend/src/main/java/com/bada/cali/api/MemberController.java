package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.MemberDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.MemberListPr;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.MemberServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Api 요청을 처리하기 위한 RestController
 */
@Tag(name = "회원", description = "로그인 · 회원가입 · 직원 관리 API")
@RequestMapping("/api/member")
@RestController("ApiMemberController")
@Log4j2
@RequiredArgsConstructor
public class MemberController {
	private final MemberServiceImpl memberService;

	@Operation(
			summary = "로그인",
			description = "Spring Security 필터가 요청을 가로채어 처리하므로 이 핸들러는 실행되지 않습니다. " +
					"실제 인증 처리는 LoginSuccessHandler / LoginFailureHandler 를 참고하세요. " +
					"요청 파라미터: username(아이디), password(비밀번호), remember-me(자동로그인, 선택)"
	)
	@PostMapping(value = "/login")
	public ResponseEntity<ResMessage<Object>> login(@RequestParam String username, @RequestParam String password) {
		log.info("[API 요청 - 로그인 요청 & 처리]");
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Operation(
			summary = "사업자번호(아이디) 중복 확인",
			description = "회원가입 시 입력한 사업자번호가 이미 등록된 아이디인지 확인합니다. " +
					"refPage=memberJoin 전달 시 중복된 업체의 업체명·주소도 함께 반환합니다."
	)
	@PostMapping(value = "/chkDuplicateLoginId")
	public ResponseEntity<ResMessage<MemberDTO.DuplicateLoginIdRes>> chkDuplicateLoginId(
			@Parameter(description = "사업자번호 (12자리, 하이픈 제외)", required = true, example = "1234567890ab")
			@RequestParam String loginId,
			@Parameter(description = "호출 페이지 구분값 (memberJoin: 업체 정보 포함 반환)", example = "memberJoin")
			@RequestParam(required = false) String refPage) {

		MemberDTO.DuplicateLoginIdRes duplicateLoginIdRes = memberService.chkDuplicateLoginId(loginId, refPage);

		int resCode = duplicateLoginIdRes.isDuplicate() ? -1 : 1;
		ResMessage<MemberDTO.DuplicateLoginIdRes> resMessage = new ResMessage<>(resCode, null, duplicateLoginIdRes);

		return ResponseEntity.ok(resMessage);
	}

	@Operation(
			summary = "회원가입 신청",
			description = "업체 정보와 담당자 정보를 등록하여 가입을 신청합니다. 관리자 승인 후 로그인이 가능합니다."
	)
	@PostMapping(value = "/memberJoin")
	public ResponseEntity<MemberDTO.MemberJoinRes> memberJoin(@RequestBody @Valid MemberDTO.MemberJoinReq memberJoinReq) {
		log.info("회원가입 요청: {}", memberJoinReq);

		MemberDTO.MemberJoinRes memberJoinRes = memberService.memberJoin(memberJoinReq);
		return ResponseEntity.ok(memberJoinRes);
	}

	@Operation(summary = "직원 목록 조회", description = "재직 상태, 검색 조건으로 직원 목록을 페이지네이션하여 조회합니다.")
	@GetMapping(value = "/getMemberList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<MemberListPr>>> getMemberList(
			@ModelAttribute MemberDTO.GetMemberListReq req
	) {
		TuiGridDTO.ResData<MemberListPr> gridData = memberService.getMemberList(req);
		TuiGridDTO.Res<TuiGridDTO.ResData<MemberListPr>> body = new TuiGridDTO.Res<>(true, gridData);
		return ResponseEntity.ok(body);
	}

	@Operation(summary = "직원 정보 등록/수정", description = "id가 없으면 신규 등록, 있으면 수정합니다.")
	@PostMapping("/memberSave")
	public ResponseEntity<ResMessage<Long>> memberSave(
			@ModelAttribute MemberDTO.SaveMemberInfo req,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		ResMessage<Long> resMessage = memberService.memberSave(req, user);
		return ResponseEntity.ok(resMessage);
	}

	@Operation(summary = "직원 상세 조회", description = "직원 고유 id로 상세 정보(기본 정보 + 서명 이미지 + 권한)를 조회합니다.")
	@GetMapping(value = "/getMemberInfo")
	public ResponseEntity<ResMessage<MemberDTO.GetMemberInfoSet>> getMemberInfo(
			@Parameter(description = "직원 고유 id", required = true, example = "1")
			@RequestParam Long id) {
		ResMessage<MemberDTO.GetMemberInfoSet> resMessage = memberService.getMemberInfo(id);
		return ResponseEntity.ok(resMessage);
	}
}
