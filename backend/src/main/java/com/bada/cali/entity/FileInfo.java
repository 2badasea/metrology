package com.bada.cali.entity;

import com.bada.cali.common.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 첨부파일 관리
@Entity
@Table(name = "file_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FileInfo {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;    // BIGINT UNSIGNED
	
	@Column(name = "ref_table_name", nullable = false, length = 50)
	private String refTableName;   // 예: "agent", "member", "report" 등
	
	@Column(name = "ref_table_id", nullable = false)
	private Long refTableId;    // 상위 테이블 PK
	
	@Column(name = "origin_name", nullable = false, length = 255)
	private String originName;     // 원본파일명 (확장자 포함)
	
	@Column(name = "name", nullable = false, length = 255)
	private String name;           // 확장자 제외 파일명
	
	@Column(name = "extension", nullable = false, length = 20)
	private String extension;      // pdf, xlsx, png ...
	
	@Column(name = "file_size", nullable = false)
	private Long fileSize;         // 바이트(byte) 단위
	
	@Column(name = "content_type", length = 100)
	private String contentType;    // 예: "application/pdf"
	
	@Column(name = "dir", nullable = false, length = 255)
	private String dir;
	
	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(name = "is_visible", nullable = false, length = 1)
	private YnType isVisible = YnType.y;    // 'y' or 'n'
	
	@Column(name = "create_datetime", nullable = false, updatable = false)
	private LocalDateTime createDatetime;
	
	@Column(name = "create_member_id", nullable = false)
	private Long createMemberId;
	
	// ★ Member와 read-only 연관관계 추가
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "create_member_id", insertable = false, updatable = false)
	private Member createMember;
	
	
	@Column(name = "update_datetime")
	private LocalDateTime updateDatetime;
	
	@Column(name = "update_member_id", nullable = false)
	@Builder.Default
	private Long updateMemberId = 0L;
	
	@Column(name = "delete_datetime")
	private LocalDateTime deleteDatetime;
	
	@Column(name = "delete_member_id", nullable = false)
	@Builder.Default
	private Long deleteMemberId = 0L;
	
}
