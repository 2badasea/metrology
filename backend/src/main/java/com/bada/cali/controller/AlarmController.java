package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 알림 관련 SSR 컨트롤러
 * - 알림 목록 모달 페이지 진입점 담당 (REST API 아님)
 */
@Controller
@RequestMapping("/alarm")
@Log4j2
public class AlarmController {

    /**
     * 알림 목록 모달
     * - topbar의 벨 아이콘 클릭 시 gModal('/alarm/list') 로 호출됨
     */
    @PostMapping("/list")
    public String alarmList() {
        return "alarm/alarmList";
    }
}
