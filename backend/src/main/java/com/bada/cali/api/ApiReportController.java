package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.ReportDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.OrderDetailsList;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.ReportServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ApiReportController {
	
	private final ReportServiceImpl reportService;
	
	// 성적서 등록
	@PostMapping(value = "/addReport")
	public ResponseEntity<ResMessage<?>> addReport(
			@RequestBody List<ReportDTO.addReportReq> reports,
			@RequestParam Long caliOrderId,		// 쿼리스트링으로 넘어오는 접수 id
			@AuthenticationPrincipal CustomUserDetails user) {
		
		Boolean resSave = reportService.addReport(reports, caliOrderId, user);
		int resCode = resSave ? 1 : -1;
		ResMessage<?> resMessage = new ResMessage<>(resCode, null, null);
		
		return ResponseEntity.ok(resMessage);
	}
	
	// 접수상세내역 내 성적서 리스트
	@GetMapping(value = "/getOrderDetailsList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<OrderDetailsList>>> getOrderDetailsList(@ModelAttribute ReportDTO.GetOrderDetailsReq req) {
		
		// 리스트 데이터 가져오기 (인터페이스 프로젝션 형태로 가져옴 )
		TuiGridDTO.ResData<OrderDetailsList> reportGridData = reportService.getOrderDetailsList(req);
		// 가져온 데이터를 바탕으로 최종 그리드 API 형식으로 세팅
		
		TuiGridDTO.Res<TuiGridDTO.ResData<OrderDetailsList>> body = new TuiGridDTO.Res<>(true, reportGridData);
		
		return ResponseEntity.ok(body);
	}
	
	
}
