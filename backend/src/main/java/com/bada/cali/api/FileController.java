package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.FileServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("ApiFileController")
@RequestMapping("/api/file")
@Log4j2
@RequiredArgsConstructor
public class FileController {
	
	private final FileServiceImpl fileService;
	
	@GetMapping("/fileDownload/{fileId}")
	public ResponseEntity<Resource> fileDownload(@PathVariable Long fileId) {
		return fileService.downloadFile(fileId);
	}

	/**
	 * 성적서 파일 전용 다운로드
	 *
	 * 성적서 파일은 file_info.id 가 아닌 report.id + 고정 파일명으로 S3 에 저장된다.
	 * fileType: "origin"(원본), "signed_xlsx"(결재 EXCEL), "signed_pdf"(결재 PDF)
	 *
	 * 호출 예: GET /api/file/report/43/origin
	 */
	@GetMapping("/report/{reportId}/{fileType}")
	public ResponseEntity<Resource> reportFileDownload(
			@PathVariable Long reportId,
			@PathVariable String fileType) {
		return fileService.downloadReportFile(reportId, fileType);
	}

	@DeleteMapping("/fileDelete/{fileId}")
	public ResponseEntity<ResMessage<?>> fileDelete(@PathVariable Long fileId, @AuthenticationPrincipal CustomUserDetails user) {
		int resCode = fileService.deleteFile(fileId, user);
		return ResponseEntity.ok(new ResMessage<>(resCode, null, null));
	}
	
}
