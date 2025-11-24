package com.bada.cali.dto;

import lombok.*;

import java.util.List;

public class TuiGridDTO<T> {
	
	// 브라우저 응답용 클래스이므로, 생성 방지
	private TuiGridDTO() {};
	
	// 요청을 받을 객체
	@Getter
	@Setter
	public static class Request {
		private int page = 1;
		private int perPage = 20;
	}
	
	// 페이지네이션 생성 객체
	@Getter @Setter
	@Builder
	public static class Pagination {
		private int page;
		private int totalCount;
	}
	
	// response 응답 객체
	@Getter @Setter
	@Builder
	public static class Response<T> {
		private List<T> contents;
		private Pagination pagination;
	}
	
	// 응답 데이터 객체를 토스트 그리드 data.api.readData 규약에 맞게 감싸기
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ResTuiGrid<T> {
		private boolean result;
		private T data;
	}
	
}
