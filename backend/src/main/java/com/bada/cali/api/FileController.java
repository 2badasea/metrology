package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.FileServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
	
	@DeleteMapping("/fileDelete/{fileId}")
	public ResponseEntity<ResMessage<?>> fileDelete(@PathVariable Long fileId, @AuthenticationPrincipal CustomUserDetails user) {
		int resCode = fileService.deleteFile(fileId, user);
		return ResponseEntity.ok(new ResMessage<>(resCode, null, null));
	}
	
}
