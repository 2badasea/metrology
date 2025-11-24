package com.bada.cali.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// view를 리턴하는 컨트롤러에서만 동작하도록 패키지 제한
@ControllerAdvice(basePackages = "com.bada.cali.controller")
public class GlobalViewNameAdvice {
	
	// viewName과 renderMode 속성값 기본값으로 추가
	@ModelAttribute
	public void addGlobalPageAttributes(HttpServletRequest request, Model model) {
		
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		
		// contextPath 제거
		if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
			uri = uri.substring(contextPath.length());
		}
		
		// 쿼리스트링 제거
		int queryIndex = uri.indexOf("?");
		if (queryIndex != -1) {
			uri = uri.substring(0, queryIndex);
		}
		// 앞의 '/' 제거
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		// model에 viewName 속성값 담기
		model.addAttribute("viewName", uri);
		
		// renderMode 세팅
		String renderMode = request.getParameter("renderMode");
		// model에 담기
		model.addAttribute("renderMode", renderMode);
		
		// 기본적으로 애플리케이션의 이름은 '교정관리'라고 표시
		model.addAttribute("title", "교정관리 프로젝트");
	}
}
