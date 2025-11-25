package com.bada.cali.dto;

import lombok.*;

import java.util.List;

public class TuiGridDTO {
	
	// 브라우저 응답용 클래스이므로, 생성 방지
	private TuiGridDTO() {};
	
	// 요청을 받을 객체
	// NOTE @ModelAttribute로 상속받아 쓰는 요청 DTO이므로 getter/setter 모두 필요
	@Getter
	@Setter
	public static class Request {
		private int page = 1;
		private int perPage = 20;
	}
	
	// 페이지네이션 생성 객체
//	@Setter	응답전용이고, 서비스에서 builder()로 만든 후 수정 안 하기 때문에 필요없음
	@Getter	// jackson 직렬화(서버->JSON)에는 getter만 있어도 충분
	@Builder
	public static class Pagination {
		private int page;
		private int totalCount;
	}
	
	// response 응답 객체
//	@Setter	위와 마찬가지로 응답전용이고, builder이후에 값을 수정하지 않음
	@Getter
	@Builder
	public static class Response<T> {
		private List<T> contents;
		private Pagination pagination;
	}
	
	// 응답 데이터 객체를 토스트 그리드 data.api.readData 규약에 맞게 감싸기
//	@Setter	=> 생성은 new ResTuiGrd<>(true, data) 한번 뿐이고, 응답전용 및 역직렬화에도 사용되지 않음
//	@NoArgsConstructor	=> 불필요(입력 DTO가 아니기 때문)
	@Getter
	@AllArgsConstructor
	public static class ResTuiGrid<T> {
		private boolean result;
		private T data;
	}
	
}
