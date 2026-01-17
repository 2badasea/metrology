package com.bada.cali.controller;

import com.bada.cali.exceptions.ForbiddenAdminModifyException;
import com.bada.cali.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/member")
@Log4j2
@RequiredArgsConstructor
public class MemberController {
	
	// 커스텀 로그인 페이지
	@GetMapping("/login")
	public String login(Model model, @AuthenticationPrincipal CustomUserDetails user) {
		log.info("login get................");
		model.addAttribute("title", "로그인");
		
		if (user != null) {
			return "redirect:/basic/home";
		}
		
		// 비로그인 상태에서만 로그인 페이지 진입 허용
		return "member/login";
	}
	
	// 회원가입 [모달]
	// NOTE 모달 페이지를 호출하는 경우 기본적으로 Post로 받는다. 모달, 일반페이지 모두 사용 시 RequestMapping을 이용하여 get과 post 모두 허용 가능
	@RequestMapping(value = "/memberJoin", method = {RequestMethod.POST, RequestMethod.GET})
	public String memberJoin(Model model) {
		log.info("memberJoin get................");
		return "member/memberJoin";
	}
	
	// 직원관리 페이지
	@GetMapping(value = "/memberManage")
	public String memberManage(Model model) {
		log.info("직원관리 페이지 이동");
		return "member/memberManage";
	}
	
	// 직원 등록/수정 페이지
	@GetMapping(value = "/memberModify")
	public String memberModify(Model model, @RequestParam(required = false) Long id, @AuthenticationPrincipal CustomUserDetails user) {
		String pageTypeKr = (id == null) ? "등록" : "수정";
		
		// 로그인 안 된 케이스가 있을 수 있으면 별도 처리(프로젝트 정책에 맞게)
		if (user == null) {
			throw new ForbiddenAdminModifyException("로그인이 필요합니다.");
		}
		
		boolean isAdmin = user.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		
		// (핵심) admin 계정(id=1) 수정 페이지 접근은 ADMIN만 허용
		if (id != null && id == 1L && !isAdmin) {
			throw new ForbiddenAdminModifyException("관리자 계정은 ADMIN만 수정할 수 있습니다.");
		}
	
		model.addAttribute("title", String.format("직원 %s", pageTypeKr));
		return "member/memberModify";
	}
	
}