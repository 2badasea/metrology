package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
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
	
}
