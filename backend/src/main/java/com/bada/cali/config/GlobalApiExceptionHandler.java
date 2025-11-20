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
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ResMessage<Object>> handleApiException(Exception exception) {
		log.info("[GlobalApiExceptionHandler]!!");
		log.info("예외메시지: " + exception.getMessage());
		ResMessage<Object> resMessage = new ResMessage<>(-1, "서버 오류가 발생했습니다.", null);
		
		// 'return ResponseEntity.status(HttpStatus.OK).body(resMessage);' 형태와 아래는 동일한 방식으로 동작
		return ResponseEntity.ok(resMessage);
	}
}
