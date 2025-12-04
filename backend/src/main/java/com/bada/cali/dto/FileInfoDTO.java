package com.bada.cali.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class FileInfoDTO {
	// 생성자 금지
	private FileInfoDTO() {
	}
	
	@Setter @Getter
	@AllArgsConstructor
	public static class FileListRes {
		private Long id;                // 파일 PK
		private String fileName;      // 파일명
		private String writerName;      // 작성자 이름
		private LocalDateTime createDatetime;// 작성일시
	}
}
