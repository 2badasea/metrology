package com.bada.cali.dto;

import com.bada.cali.common.enums.AlarmType;
import com.bada.cali.common.enums.YnType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 알림 관련 DTO 클래스 집합
public class AlarmDTO {

    // 임의 생성자 호출 방지
    private AlarmDTO() {}

    // 알림 목록 단건 응답
    @Schema(description = "알림 목록 단건")
    public record ListItem(
            @Schema(description = "알림 ID") Long id,
            @Schema(description = "알림 유형") AlarmType alarmType,
            @Schema(description = "참조 대상 종류 (WORK/REPORT/BOARD_NOTICE)") String refType,
            @Schema(description = "참조 대상 ID") Long refId,
            @Schema(description = "알림 내용 (제목 겸 본문 스냅샷)") String content,
            @Schema(description = "발신자명 (NULL이면 시스템)") String senderName,
            @Schema(description = "읽음 여부 (y/n)") YnType isRead,
            @Schema(description = "알림 생성일시") LocalDateTime createDatetime
    ) {}

    // 미읽음 카운트 응답
    @Schema(description = "미읽음 알림 카운트")
    public record CountRes(
            @Schema(description = "미읽음 알림 수") long unreadCount
    ) {}

    // ref 기준 일괄 읽음 처리 요청
    @Schema(description = "ref 기준 일괄 읽음 요청")
    public record ReadByRefReq(
            @Schema(description = "참조 대상 종류 (WORK/REPORT/BOARD_NOTICE)") String refType,
            @Schema(description = "참조 대상 ID") Long refId
    ) {}

    // 대시보드 → cali webhook 수신 DTO
    // 대시보드가 댓글/공지 등록 후 POST /api/callback/alarm 으로 전송하는 페이로드
    @Schema(description = "대시보드 알림 webhook 수신 페이로드")
    public record DashboardCallbackReq(
            @Schema(description = "수신자 member.id") Long memberId,
            @Schema(description = "알림 유형 (WORK_COMMENT / WORK_NOTICE)") AlarmType alarmType,
            @Schema(description = "참조 대상 종류 (항상 WORK)") String refType,
            @Schema(description = "참조 대상 ID (work.id)") Long refId,
            @Schema(description = "알림 내용 (제목 겸 본문 스냅샷)") String content,
            @Schema(description = "발신자명 (개발팀 등)") String senderName
    ) {}
}
