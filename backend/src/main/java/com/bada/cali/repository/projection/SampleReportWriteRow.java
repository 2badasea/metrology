package com.bada.cali.repository.projection;

import java.time.LocalDateTime;

/**
 * 성적서작성 모달 — 샘플 파일 목록 조회용 Projection (native query 기반)
 * - sample + file_info + member JOIN
 * - 소분류 코드 기준으로 해당 소분류의 샘플별 업로드된 파일 1건씩 표시
 */
public interface SampleReportWriteRow {

	Long getId();                    // file_info.id (파일 고유 id)

	Long getSampleId();              // sample.id

	String getItemName();            // sample.name (기기명)

	String getFileName();            // file_info.origin_name (원본파일명)

	String getCreateMemberName();    // member.name (등록자 이름)

	LocalDateTime getCreateDatetime(); // file_info.create_datetime (등록일시)
}