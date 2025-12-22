package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

@Controller
@RequestMapping("/cali")
@Log4j2
public class CaliController {
	
	// 교정접수 페이지 이동
	@GetMapping(value = "/caliOrder")
	public String caliOrder(Model model) {
		model.addAttribute("title", "교정접수");
		
		// 검색시작일, 검색종료일 반환
		// NOTE LocalDate 타입으로 model에 담아서 타임리프선에서 파싱하는 방법과 String으로 보내는 방법 존재
		// 1. LocalDate 타입으로 생성 (NOTATION 41번으로 정리. 날짜 구하는 방식)
		LocalDate today = LocalDate.now();
		LocalDate orderStartDate = today.minusMonths(3).withDayOfMonth(1);
		LocalDate orderEndDate = today.with(TemporalAdjusters.lastDayOfMonth());
		
		// String으로 보낸다면 아래처럼 형변환 후 전달
		// String startDate = orderStartDate.toString();
		// String endDate = orderEndDate.toString();
		
		// model에 담아서 리턴 -> 타임리프를 통해 값을 format하여 표시한다.
		model.addAttribute("orderStartDate", orderStartDate);
		model.addAttribute("orderEndDate", orderEndDate);
		
		return "cali/caliOrder";
	}
	
	// 교정접수 등록/수정 페이지 [모달]
	@PostMapping(value = "/caliOrderModify")
	public String caliOrderModify(Model model, @RequestParam(required = false) Long id) {
		boolean isModify = (id != null && id > 0);
		model.addAttribute("isModify", isModify);
		return "cali/caliOrderModify";
	}
	
	// 접수상세내역
	@GetMapping(value = "/orderDetails")
	public String orderDetails(Model model, @RequestParam long caliOrderId) {
		model.addAttribute("title", "교정접수내역");
		model.addAttribute("caliOrderId", caliOrderId);
		return "cali/orderDetails";
	}
}
