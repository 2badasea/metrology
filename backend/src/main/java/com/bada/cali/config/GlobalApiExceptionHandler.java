package com.bada.cali.config;

import com.bada.cali.common.ResMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API 요청을 처리하는 과정에서 발생하는 예외들 공통 처리
 */
@RestControllerAdvice(annotations = RestController.class)
@Log4j2
public class GlobalApiExceptionHandler {
	
	// FIX 대표적인 구체예외들에 대해선 케이스별로 구분한 뒤, 아래 메서드는 최종 방패용으로써 역할을 하도록 할 것 (비즈니스 로직 실패 ex) 잔액부족, 로그인ID 중복 등에 대해서도 전부 '서버 에러'처럼 표시될 수 있음
//	실무에서 많이 쓰는 구조
//	보통 이런 식으로 나눠:
//	유효성 검증/요청 문제 → 400 BAD_REQUEST
//	인증/인가 문제 → 401 / 403
//	비즈니스 예외 → 400 또는 도메인에 맞는 상태
//	나머지 전부 → 500 (마지막 방패: Exception.class)
	// TODO 추후 각 예외에 맞는 커스텀 메시지를 구현하기 위해 커스텀 예외클래스에 대한 패키지를 별도로 만들어서 관리할 것
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ResMessage<Object>> handleApiException(Exception exception) {
		log.info("[GlobalApiExceptionHandler]!!");
		log.info("예외메시지: {}", exception.getMessage());
		ResMessage<Object> resMessage = new ResMessage<>(-1, "서버 오류가 발생했습니다.", null);
		
		// 'return ResponseEntity.status(HttpStatus.OK).body(resMessage);' 형태와 아래는 동일한 방식으로 동작
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resMessage);
	}
}
