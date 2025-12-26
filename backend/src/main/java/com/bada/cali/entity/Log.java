package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {
	
	// LOG 고유번호
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "INT UNSIGNED")
	private Long id;
	
	// LOG ip
	@Column(name = "log_ip", length = 50)
	private String logIp;
	
	// 작업자 명
	@Column(name = "worker_name", length = 50, nullable = false)
	private String workerName;
	
	// LOG 이력내용
	@Lob
	@Column(name = "log_content")
	private String logContent;
	
	// 로그타입 (i:등록, u:수정, d:삭제, l:로그인)
	@Column(name = "log_type", length = 50)
	private String logType;
	
	// 참조 테이블명
	@Column(name = "ref_table", length = 50)
	private String refTable;
	
	// 참조 테이블 id (DB에선 해당 필드에 NULL을 허용한다)
	@Column(name = "ref_table_id", nullable = true, columnDefinition = "INT UNSIGNED DEFAULT 0")
	@Builder.Default	// 명시하지 않으면 builder 객체 사용 시, 값을 주지 않을 경우 에러가 발생 (NOT NLL 제약조건)
	private Long refTableId = 0L;
	
	// 등록일시
	@Column(name = "create_datetime", nullable = true, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime createDatetime;
	
	// 등록자
	@Column(name = "create_member_id", nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
	private Long createMemberId = 0L;
}
