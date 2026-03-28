package com.bada.cali.service;

import com.bada.cali.common.enums.AlarmType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.AlarmDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Alarm;
import com.bada.cali.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl {

    private final AlarmRepository alarmRepository;

    // ─── 조회 ──────────────────────────────────────────────────────────────

    /**
     * 미읽음 알림 수 반환
     */
    @Transactional(readOnly = true)
    public long countUnread(Long memberId) {
        return alarmRepository.countByMemberIdAndIsRead(memberId, YnType.n);
    }

    /**
     * 알림 목록 반환 (TuiGrid 서버 페이징 포맷)
     * @param req       TuiGrid 페이징 요청 (1-based page, perPage)
     * @param memberId  로그인 사용자 ID
     * @param alarmType null이면 전체, 값이 있으면 해당 유형만 조회
     */
    @Transactional(readOnly = true)
    public TuiGridDTO.ResData<AlarmDTO.ListItem> getAlarmList(TuiGridDTO.Request req,
                                                              Long memberId,
                                                              AlarmType alarmType) {
        // TuiGrid는 1-based page, Spring Data는 0-based
        PageRequest pageable = PageRequest.of(req.getPage() - 1, req.getPerPage());

        Page<Alarm> page = (alarmType != null)
                ? alarmRepository.findByMemberIdAndAlarmTypeOrderByCreateDatetimeDesc(memberId, alarmType, pageable)
                : alarmRepository.findByMemberIdOrderByCreateDatetimeDesc(memberId, pageable);

        List<AlarmDTO.ListItem> contents = page.getContent().stream()
                .map(a -> new AlarmDTO.ListItem(
                        a.getId(),
                        a.getAlarmType(),
                        a.getRefType(),
                        a.getRefId(),
                        a.getContent(),
                        a.getSenderName(),
                        a.getIsRead(),
                        a.getCreateDatetime()
                ))
                .toList();

        TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
                .page(req.getPage())
                .totalCount((int) page.getTotalElements())
                .build();

        return TuiGridDTO.ResData.<AlarmDTO.ListItem>builder()
                .contents(contents)
                .pagination(pagination)
                .build();
    }

    // ─── 읽음 처리 ─────────────────────────────────────────────────────────

    /**
     * 개별 읽음 처리
     */
    @Transactional
    public void markReadById(Long id, Long memberId) {
        alarmRepository.markReadById(id, memberId, LocalDateTime.now());
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public void markReadAll(Long memberId) {
        alarmRepository.markReadAll(memberId, LocalDateTime.now());
    }

    /**
     * ref 기준 일괄 읽음 처리
     * 개발팀 문의 상세 조회 시 해당 work 관련 알림을 자동으로 읽음 처리
     */
    @Transactional
    public void markReadByRef(Long memberId, String refType, Long refId) {
        alarmRepository.markReadByRef(memberId, refType, refId, LocalDateTime.now());
    }

    // ─── 알림 생성 (내부 호출용) ────────────────────────────────────────────

    /**
     * 알림 단건 생성
     * @param memberId   수신자 member.id
     * @param alarmType  알림 유형
     * @param refType    참조 대상 종류 (WORK / REPORT / BOARD_NOTICE)
     * @param refId      참조 대상 ID
     * @param content    알림 내용 (제목 겸 본문 스냅샷)
     * @param senderName 발신자명 (null이면 시스템 발신)
     */
    @Transactional
    public Alarm createAlarm(Long memberId, AlarmType alarmType,
                             String refType, Long refId,
                             String content, String senderName) {
        Alarm alarm = Alarm.builder()
                .memberId(memberId)
                .alarmType(alarmType)
                .refType(refType)
                .refId(refId)
                .content(content)
                .senderName(senderName)
                .build();
        return alarmRepository.save(alarm);
    }

    // ─── 배치: 오래된 알림 정리 ─────────────────────────────────────────────

    /**
     * 매일 새벽 2시: 오래된 알림 자동 삭제
     * - 읽은 알림 : 30일 경과 후 삭제
     * - 미읽음 알림: 90일 경과 후 삭제
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldAlarms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffRead   = now.minusDays(30);
        LocalDateTime cutoffUnread = now.minusDays(90);

        int deletedRead   = alarmRepository.deleteReadAlarmsBefore(cutoffRead);
        int deletedUnread = alarmRepository.deleteUnreadAlarmsBefore(cutoffUnread);

        log.info("[AlarmCleanup] 읽음 {}건 / 미읽음 {}건 삭제 완료", deletedRead, deletedUnread);
    }
}
