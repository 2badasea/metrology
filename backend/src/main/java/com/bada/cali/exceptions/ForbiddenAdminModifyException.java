package com.bada.cali.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN) // 선택: API가 아니라 view라도 상태코드 403으로 세팅됨
public class ForbiddenAdminModifyException extends RuntimeException {
	public ForbiddenAdminModifyException(String message) {
		super(message);
	}
}
