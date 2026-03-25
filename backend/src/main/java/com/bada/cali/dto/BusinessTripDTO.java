package com.bada.cali.dto;

import com.bada.cali.dto.EquipmentDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

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
            String updateMemberName       // 최종수정자 이름
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
}
