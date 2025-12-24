package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.ReportDTO;
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
	
}
