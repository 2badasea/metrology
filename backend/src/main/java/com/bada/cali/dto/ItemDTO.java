package com.bada.cali.dto;

import com.bada.cali.common.enums.YnType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ItemDTO {
	
	// 네임스페이스 용도. 생성자 선언 불가
	private ItemDTO() {
	}
	
	;
	
	@Getter
	@Setter
	public static class ListReq extends TuiGridDTO.Request {
		// 검색창에서 넘어오는 값들
		private YnType isInhousePossible;    // 당사가능여부 (전체일 경우 null로 초기화됨)
		private Long middleItemCodeId;        // 중분류코드id
		private Long smallItemCodeId;        // 소분류코드id
		private String name;        // 품목명
		private String makeAgent;        // 제작회사
		private String format;        // 형식
	}
	
	// 품목정보 record
	public record ItemData(
			Long id,
			Long middleItemCodeId,
			Long smallItemCodeId,
			String name,
			String nameEn,
			String makeAgent,
			String makeAgentEn,
			String format,
			String num,
			Long fee,
			Integer caliCycle,
			YnType isInhousePossible
	) {
	}
	
	public record SaveItemData(
			ItemDTO.ItemData itemData,
			List<Long> delHistoryIds,
			List<ItemFeeData> itemFeeHistoryList
	
	) {
	}
	
	// 품목수수료 데이터
	public record ItemFeeData(
			Long id,
			Long itemId,
			LocalDate baseDate,
			Long baseFee,
			String remark
	) {
	}
	
	// 삭제대상 품목 정보
	public record DeleteItemData(
			Long id,
			String name
	) {
	}
	
	// 품목정보 중복체크용 데이터 가공(기기명, 제작회사, 형식)
	public record ItemCheckData(
			String name,
			String makeAgent,
			String format
	) {
		// 생성자
		public ItemCheckData {
			name = normedData(name);
			makeAgent = normedData(makeAgent);
			format = normedData(format);
		}
		
		// 클라이언트 단에서도 trim()을 처리해주지만, 서버에서 한번 더 처리해준다.
		private static String normedData(String s) {
			return s == null ? "" : s.trim();
		}
	}
	
	// 중복체크용 데이터의 가공된 형태와 교정수수료 묶음
	public record FindOrInsertItemParams(ItemCheckData itemCheckData, Long fee, Long middleItemCodeId, Long smallItemCodeId, Integer caliCycle) {
	}
	
	
}
