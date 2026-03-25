package com.bada.cali.dto;

import com.bada.cali.dto.EquipmentDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 출장일정 DTO
 */
public class BusinessTripDTO {

    /**
     * 캘린더 기간 조회 요청
     * FullCalendar는 현재 뷰 기간의 start/end를 ISO8601 문자열로 전달함
     */
    public record CalendarReq(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {}

    /**
     * 캘린더 이벤트 단건 응답
     * - FullCalendar 이벤트 형식 + 추가 메타 필드
     */
    public record CalendarEventRes(
            Long id,
            String type,
            String title,
            LocalDateTime startDatetime,
            LocalDateTime endDatetime,
            String custAgent,
            String travelerIds,           // 콤마 구분 문자열 ("1,5,12")
            String updateMemberName       // 최종수정자 이름
    ) {}

    /**
     * 출장일정 상세 조회 응답 (수정 모달 초기 데이터 로드용)
     */
    public record DetailRes(
            Long id,
            String type,
            String title,
            LocalDateTime startDatetime,
            LocalDateTime endDatetime,
            Long custAgentId,             // 신청업체 agent.id
            String custAgent,
            String custAgentAddr,
            String custManager,
            String custManagerTel,
            String custManagerEmail,
            Long reportAgentId,           // 성적서발행처 agent.id
            String reportAgent,
            String reportAgentAddr,
            String reportManager,
            String reportManagerTel,
            String siteAddr,
            String siteManager,
            String siteManagerTel,
            String siteManagerEmail,
            String travelerIds,
            String carIds,
            String remark,
            String updateMemberName,      // 최종수정자 이름
            long fileCnt                  // 첨부파일 건수 (버튼 색상 표시용)
    ) {}

    /**
     * 출장일정 등록 요청
     */
    public record CreateReq(
            @NotBlank String title,
            String type,
            @NotNull LocalDateTime startDatetime,
            @NotNull LocalDateTime endDatetime,
            Long custAgentId,
            String custAgent,
            String custAgentAddr,
            String custManager,
            String custManagerTel,
            String custManagerEmail,
            Long reportAgentId,
            String reportAgent,
            String reportAgentAddr,
            String reportManager,
            String reportManagerTel,
            String siteAddr,
            String siteManager,
            String siteManagerTel,
            String siteManagerEmail,
            String travelerIds,
            String remark,
            List<EquipmentDTO.UsedEquipment> equipmentDatas   // 사용 표준장비 목록 (없으면 null 또는 빈 리스트)
    ) {}

    /**
     * 출장일정 수정 요청
     */
    public record UpdateReq(
            @NotBlank String title,
            String type,
            @NotNull LocalDateTime startDatetime,
            @NotNull LocalDateTime endDatetime,
            Long custAgentId,
            String custAgent,
            String custAgentAddr,
            String custManager,
            String custManagerTel,
            String custManagerEmail,
            Long reportAgentId,
            String reportAgent,
            String reportAgentAddr,
            String reportManager,
            String reportManagerTel,
            String siteAddr,
            String siteManager,
            String siteManagerTel,
            String siteManagerEmail,
            String travelerIds,
            String remark,
            List<EquipmentDTO.UsedEquipment> equipmentDatas   // 사용 표준장비 목록 (null이면 기존 유지, 빈 리스트면 전체 삭제)
    ) {}

    /**
     * 출장자 select 옵션용 (member 목록)
     */
    public record MemberOption(
            Long id,
            String name
    ) {}

    /**
     * 중복 체크 요청
     * - btripId: 수정 시 자기 자신 제외용 (신규 등록 시 null)
     * - equipmentIds: 현재 그리드에 담긴 장비 id 목록
     */
    public record ConflictCheckReq(
            Long btripId,
            @NotNull LocalDateTime startDatetime,
            @NotNull LocalDateTime endDatetime,
            List<Long> equipmentIds
    ) {}

    /**
     * 중복 체크 응답
     * - hasConflict: 중복 여부
     * - conflictEquipments: 중복된 장비 목록 (그리드에서 제거 대상)
     */
    public record ConflictCheckRes(
            boolean hasConflict,
            List<ConflictEquipmentItem> conflictEquipments
    ) {
        /**
         * 중복 장비 단건
         * - conflictInfo: "출장제목 (M/d HH:mm~HH:mm)" 포맷 — 프론트 gMessage 표시용
         */
        public record ConflictEquipmentItem(
                Long equipmentId,
                String name,
                String manageNo,
                String conflictInfo   // "출장일정명 (3/25 16:00~17:00)"
        ) {}
    }

    /**
     * 리스트 조회 요청 (TUI Grid 서버사이드 페이지네이션 표준 패턴)
     * - TuiGridDTO.Request에서 page(기본 1), perPage(기본 20) 상속
     * - searchType: 검색 대상 컬럼 (title | custAgent | custAgentAddr | reportAgent | reportAgentAddr)
     * - keyword: 검색 키워드 (없으면 전체 조회)
     * - dateStart/dateEnd: 출장시작일시 기준 날짜 범위 (yyyy-MM-dd 문자열, null이면 범위 제한 없음)
     */
    @Getter
    @Setter
    public static class GetListReq extends TuiGridDTO.Request {
        private String searchType;
        private String keyword;
        private String dateStart;
        private String dateEnd;
    }

    /**
     * 출장일정 일괄 삭제 요청
     * - ids: 삭제할 출장일정 id 목록 (1개 이상 필수)
     */
    public record DeleteReq(
            @NotEmpty(message = "삭제할 항목을 선택하세요.")
            List<Long> ids
    ) {}
}
