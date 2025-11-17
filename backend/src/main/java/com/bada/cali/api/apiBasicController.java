package com.bada.cali.api;

import com.bada.cali.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController        // @Controller + @ResponseBody의 조합으로, 자동으로 응답 데이터를 JSON 형태로 직렬화해서 리턴한다.
@RequestMapping("/apiBasic")
@Log4j2
@RequiredArgsConstructor
public class apiBasicController {
	
	// DAO 초기화
	private final MemberRepository memberRepository;    // Member 리파지토리
	
}
