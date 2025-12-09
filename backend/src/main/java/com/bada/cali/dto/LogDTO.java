package com.bada.cali.dto;

import com.bada.cali.entity.Log;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogDTO {
	
	private Long id;              // LOG 고유번호
	private String logIp;            // LOG ip
	private String workerName;       // 작업자 명
	private String logContent;       // LOG 이력내용
	private String logType;          // 로그타입 (i:등록, u:수정, d:삭제, l:로그인)
	private String refTable;         // 참조 테이블명
	private Long refTableId;      // 참조 테이블 idx
	private LocalDateTime createDatetime; // 등록일시
	private Long createMemberId;  // 등록자
	
	// 엔티티 → DTO 변환 메서드
	public static LogDTO fromEntity(Log log) {
		return LogDTO.builder()
				.id(log.getId())
				.logIp(log.getLogIp())
				.workerName(log.getWorkerName())
				.logContent(log.getLogContent())
				.logType(log.getLogType())
				.refTable(log.getRefTable())
				.refTableId(log.getRefTableId())
				.createDatetime(log.getCreateDatetime())
				.createMemberId(log.getCreateMemberId())
				.build();
	}
}


