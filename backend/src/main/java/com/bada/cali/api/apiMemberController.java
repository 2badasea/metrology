package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.ResDuplicateLoginIdDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.repository.MemberRepository;
import com.bada.cali.service.MemberServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Api 요청을 처리하기 위한 RestController
 */
@RequestMapping("/apiMember")
@RestController
@Log4j2
@RequiredArgsConstructor
public class apiMemberController {
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
	public ResponseEntity<ResMessage<ResDuplicateLoginIdDTO>> chkDuplicateLoginId(@RequestParam String loginId, @RequestParam(required = false) String refPage) {
		log.info("loginId= {}, refPage= {}", loginId, refPage);
		
		// loginId 중복여부 조회 (서비스 계층에선 중복여부 + 업체정보만 책임)
		ResDuplicateLoginIdDTO resDuplicateLoginIdDTO = memberService.chkDuplicateLoginId(loginId, refPage);
		
		// DTO에서의 boolean 타입 필드를 꺼내려고 할 때는 메서드를 호출하는 형태로 가져온다.
		int resCode = resDuplicateLoginIdDTO.isDuplicate() ? -1 : 1;
		String resMsg = resCode > 0 ? "사용 가능한 아이디입니다." : "이미 가입되어 있는 정보입니다.";
		ResMessage<ResDuplicateLoginIdDTO> resMessage = new ResMessage<>(resCode, resMsg, resDuplicateLoginIdDTO);
		
		// ok(...)은 "200 OK + body 설정"을 한 번에 해주는 축약형 return 'ReponseEntity.status(HttpStatus.OK).body(resMessage)'과 동일
		return ResponseEntity.ok(resMessage);
	}
	
}
