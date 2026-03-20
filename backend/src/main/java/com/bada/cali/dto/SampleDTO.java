package com.bada.cali.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class SampleDTO {

	private SampleDTO() {}

	// 샘플 목록 조회 요청 (Toast UI Grid 서버 페이징)
	@Getter
	@Setter
	public static class GetListReq extends TuiGridDTO.Request {
		private Long middleItemCodeId;
		private Long smallItemCodeId;
		private String searchType = "all";
		private String keyword = "";
	}

	// 샘플 등록/수정 요청
	public record SampleSaveReq(
			String name,
			Long middleItemCodeId,
			Long smallItemCodeId
	) {}

	// 샘플 삭제 요청
	public record DeleteReq(
			List<Long> ids
	) {}

	// 중복 체크 응답
	public record DuplicateCheckRes(
			boolean exists,
			Long sampleId
	) {}

	// 성적서작성 모달 — 샘플 파일 목록 조회 요청 (페이지네이션 없이 전체 조회)
	@Getter
	@Setter
	public static class GetReportWriteListReq {
		private Long smallItemCodeId;         // 소분류 코드 id (필수)
		private String searchType = "all";    // 검색 타입: all, itemName, fileName, createMemberName
		private String keyword = "";          // 검색 키워드
	}
}
