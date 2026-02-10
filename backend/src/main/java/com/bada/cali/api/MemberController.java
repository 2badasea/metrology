package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.MemberDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.MemberListPr;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.MemberServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Api 요청을 처리하기 위한 RestController
 */
@RequestMapping("/api/member")
@RestController("ApiMemberController")
@Log4j2
@RequiredArgsConstructor
public class MemberController {
	private final MemberServiceImpl memberService;
	
	/**
	 * 로그인 요청 api.
	 * // NOTE 시큐리티 설정으로 인해 /apiMember/login 요청을 가로채어 처리하기 때문에 아래 핸들러에서 처리되진 않음.
	 *
	 * @param username
	 * @param password
	 * @return
	 */
	@PostMapping(value = "/login")
	public ResponseEntity<ResMessage<Object>> login(@RequestParam String username, @RequestParam String password) {
		log.info("[API 요청 - 로그인 요청 & 처리]");
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	/**
	 * 로그인ID 중복여부 체크
	 * @param loginId
	 * @return
	 */
	@PostMapping(value = "/chkDuplicateLoginId")
	public ResponseEntity<ResMessage<MemberDTO.DuplicateLoginIdRes>> chkDuplicateLoginId(@RequestParam String loginId, @RequestParam(required = false) String refPage) {
		
		// loginId 중복여부 조회 (서비스 계층에선 중복여부 + 업체정보만 책임)
		MemberDTO.DuplicateLoginIdRes duplicateLoginIdRes = memberService.chkDuplicateLoginId(loginId, refPage);
		
		// DTO에서의 boolean 타입 필드를 꺼내려고 할 때는 메서드를 호출하는 형태로 가져온다.
		int resCode = duplicateLoginIdRes.isDuplicate() ? -1 : 1;	// 1: 중복없음(가입가능)
		ResMessage<MemberDTO.DuplicateLoginIdRes> resMessage = new ResMessage<>(resCode, null, duplicateLoginIdRes);
		
		// ok(...)은 "200 OK + body 설정"을 한 번에 해주는 축약형 return 'ReponseEntity.status(HttpStatus.OK).body(resMessage)'과 동일
		return ResponseEntity.ok(resMessage);
	}
	
	/**
	 * 회원가입 처리
	 * FIX 응답 DTO로써 특별히 데이터를 넘기는 게 아닌 경우, DTO 클래스를 양산하는 것보단 ResMessage 응답 클래스를 활용할 것 (무분별한 DTO 생성 방지)
	 * @param memberJoinReq
	 * @return
	 */
	@PostMapping(value = "/memberJoin")
	public ResponseEntity<MemberDTO.MemberJoinRes> memberJoin(@RequestBody MemberDTO.MemberJoinReq memberJoinReq) {
		// cf) dto의 값이 null일 때, dto.toString()은 NPE 일으킴. log.info를 활용하면 null이라도 "null" 형태로 반환
		log.info("회원가입 요청: {}", memberJoinReq);
		
		MemberDTO.MemberJoinRes memberJoinRes = memberService.memberJoin(memberJoinReq);
		return ResponseEntity.ok(memberJoinRes);
	}
	
	// 직원관리 리스트 가져오기
	@GetMapping(value = "/getMemberList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<MemberListPr>>> getMemberList(
			@ModelAttribute MemberDTO.GetMemberListReq req
	) {
		
		TuiGridDTO.ResData<MemberListPr> gridData = memberService.getMemberList(req);
		TuiGridDTO.Res<TuiGridDTO.ResData<MemberListPr>> body = new TuiGridDTO.Res<>(true, gridData);
		return ResponseEntity.ok(body);
	}
	
	// 직원정보 등록하기
	@PostMapping("/memberSave")
	public ResponseEntity<ResMessage<Long>> memberSave(
			@ModelAttribute MemberDTO.SaveMemberInfo req,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		ResMessage<Long> resMessage = memberService.memberSave(req, user);
		return ResponseEntity.ok(resMessage);
	}
	
	// 직원정보 가져오기
	@GetMapping(value = "/getMemberInfo")
	public ResponseEntity<ResMessage<MemberDTO.GetMemberInfoSet>> getMemberInfo(@RequestParam Long id) {
		ResMessage<MemberDTO.GetMemberInfoSet> resMessage = memberService.getMemberInfo(id);
		return ResponseEntity.ok(resMessage);
	}
}
