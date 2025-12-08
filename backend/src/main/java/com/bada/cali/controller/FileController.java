package com.bada.cali.controller;

import com.bada.cali.common.ResMessage;
import com.bada.cali.entity.FileInfo;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.FileServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/file")
@AllArgsConstructor
@Log4j2
public class FileController {
	
	private final FileServiceImpl fileService;
	
	// 첨부파일 다운로드
	// Resource 패키지 확인
	@GetMapping(value = "fileDown/{fileId}")
	public ResponseEntity<Resource> fileDown(@PathVariable long fileId) {
		// 서비스로직에서 바로 처리하기
		return fileService.downloadFile(fileId);
	}
	
	// 첨부파일 삭제처리
	@PostMapping("/fileDelete/{fileId}")
	public ResponseEntity<ResMessage<?>> fileDelete(@PathVariable Long fileId, @AuthenticationPrincipal CustomUserDetails user) {
		log.info("파일 아이디 확인: {}", fileId);
		int resCode = fileService.deleteFile(fileId, user);
		log.info("resCode: {}", resCode);
		return ResponseEntity.ok(new ResMessage<>(resCode, null, null));
	}
}
