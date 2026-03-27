package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 개발팀 문의 관련 SSR 컨트롤러
 * - 모달 페이지 진입점 담당 (REST API 아님)
 */
@Controller
@RequestMapping("/dev")
@Log4j2
public class DevController {

    /**
     * 개발팀 문의 모달
     * - topbar의 '개발팀 문의' 클릭 시 gModal('/dev/workModify') 로 호출됨
     * - workId 있으면 상세/수정, 없으면 목록 진입
     */
    @PostMapping("/workModify")
    public String workModify(@RequestParam(required = false) Long workId, Model model) {
        model.addAttribute("workId", workId);
        return "dev/workModify";
    }
}
