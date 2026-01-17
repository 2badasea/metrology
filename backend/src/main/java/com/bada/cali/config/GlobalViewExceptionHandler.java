package com.bada.cali.config;

import com.bada.cali.exceptions.ForbiddenAdminModifyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 일반 페이지/모달 뷰를 호출하는 부분에서 예외가 터지는 것에 대한 전역 처리
 */
@ControllerAdvice(annotations = Controller.class)
@Log4j2
public class GlobalViewExceptionHandler {
	
	@ExceptionHandler(Exception.class)
	public String handleException(
			Exception exception,
			Model model,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes
	) {
		
		// 로그 (stack trace 포함)
		log.error("[GlobalViewExceptionHandler] url = {}", request.getRequestURI(), exception);
		
		// 화면에서 쓸 정보 세팅
		model.addAttribute("errMsg", "요청을 처리하는 중 오류가 발생했습니다.");
		model.addAttribute("path", request.getRequestURI());
		
		// 500 에러페이지는 일반 view를 리턴해도 무관. redirect를 하기 위해선 컨트롤러에 별도로 매핑을 시켜야 함.(오버스펙)
		return "error/500";
	}
	
	@ExceptionHandler(ForbiddenAdminModifyException.class)
	public String handleForbiddenAdminModify(
			ForbiddenAdminModifyException ex,
			Model model,
			HttpServletRequest request
	) {
		model.addAttribute("errMsg", ex.getMessage());
		model.addAttribute("path", request.getRequestURI());
		return "error/403";
	}
	
}
