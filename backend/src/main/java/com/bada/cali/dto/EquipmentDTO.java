package com.bada.cali.dto;

import com.bada.cali.common.enums.YnType;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public class EquipmentDTO {
	
	// 본 DTO클래스는 네임스페이스 용도
	private EquipmentDTO () {
	}
	
	// 표준장비 분야 코드 데이터
	public record EquipFieldData (
			Long id,
			String name,
			String code,
			YnType isUse,
			String remark
	) {
	}
	
	@Getter
	@Setter
	public static class GetLiseReq extends TuiGridDTO.Request {
		YnType isUse;				// 사용여부(유휴여부)
		YnType isDispose;			// 폐기여부
		Long equipmentFieldId;		// 분야코드 id
		String searchType;			// 검색타입
		String keyword;				// 검색키워드
		List<Long> exceptIds;		// 검색 제외 id
	}
	
	// 표준장비 등록/수정 데이터
	public record EquipmentData(
			Long id,
			String manageNo,				// 관리번호
			String name,
			String nameEn,
			String makeAgent,
			String makeAgentEn,
			String serialNo,				// 기기번호
			Long equipmentFieldId,			// 분야 id
			YnType isDispose,				// 폐기여부
			YnType isInterimCheck,			// 중간점검여부
			String modelName,
			Integer caliCycleMonths,
			String accuracySpec,			// 상세 스펙
			String purpose,					// 용도, 목적
			Long primaryManagerId,			// 담당(정)
			Long secondaryManagerId,		// 담당(부)
			Long manageDepartmentId,
			String installLocation,			// 설치 장소
			Integer dueNotifyDays,			// 도래알림일
			String purchaseVendor,			// 구입처
			String purchaseVendorTel,		// 구입처 연락처
			String purchaseContactManager,	// 구입처 담당자
			YnType isUse,
			LocalDate purchaseDate,			// 구입일
			LocalDate installDate,			// 설치일
			String purchasePrice,
			String detailNote,				// 세부사항
			YnType hasManual,				// 매뉴얼 유무
			YnType isInstallCondition,		// 설치상태
			YnType hasFirmware,				// 펌웨어 여부
			String firmwareVersion,			// 펌웨어 버전
			String property,				// 특성
			String remark,					// 비고
			
			String equipImgPath,		// 첨부파일 이미지 경로
			Integer fileCnt,				// 파일 개수
			MultipartFile equipmentImgFile,	// 업로드 이미지 파일
			List<MultipartFile> equipmentFiles	// 첨부파일
	) {}
	
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class GetEquipInfos {
		EquipmentData data;
		Integer uploadFileCnt;
		String equipImgPath;
	}
	
	// 삭제 요청 표준장비 ids
	public record DeleteEquipmentReq(
			List<Long> deletedIds
	){};
	
	@Setter
	@Getter
	@NoArgsConstructor
	public static class GetUsedListReq extends TuiGridDTO.Request {
		Long refTableId;
		String refTable;
	}
	
	// 사용중인 표준장비
	public record UsedEquipment (
			String refTable,
			Long refTableId,
			Long equipmentId,
			Integer seq
	){}
	
	
	
}
