package com.bada.cali.config;

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
			HttpServletRequest request,
			RedirectAttributes redirectAttributes,
			Authentication authentication
	) {
		// 로그인 여부 판단
		boolean chkLoggedIn = authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
		
		// 로그 남기기
		log.error("[GlobalViewExceptionHandler] url = {}", request.getRequestURI());
		
		// TODO 존재하지 않는 페이지에 접근할 때도 구분할 것 => 이경우, 별도의 예외페이지 생성할 것 (single layout && Page Not Found, HOME으로 가기 기능 구현)
		
		// 로그인을 하지 않은 상황에서 접근하는 경우엔 security 에서 처리를 하기 때문에 addFlashAttribute로 처리해도 동작하지 X
		if (!chkLoggedIn) {
			// 리다이렉트하는 URL의 파라미터에 Model 객체가 명시되어 있지 않더라도, 자동으로 model에 실려서 리다이렉트된로 전달됨
			redirectAttributes.addFlashAttribute("errorMsg", "로그인 후 이용해주세요.");
			return "redirect:/member/login";
		}
		
		redirectAttributes.addFlashAttribute("errorMsg", "처리 중 오류가 발생했습니다.");
		return "redirect:/";
	}
	
	
}
