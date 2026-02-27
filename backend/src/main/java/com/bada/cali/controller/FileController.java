package com.bada.cali.controller;

import com.bada.cali.service.FileServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/file")
@AllArgsConstructor
@Log4j2
public class FileController {

	private final FileServiceImpl fileService;

	// 첨부파일 다운로드
	@GetMapping(value = "fileDown/{fileId}")
	public ResponseEntity<Resource> fileDown(@PathVariable long fileId) {
		return fileService.downloadFile(fileId);
	}
}
