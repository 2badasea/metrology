package com.bada.cali.service;

import com.bada.cali.dto.BusinessTripDTO;
import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.BusinessTrip;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.StandardEquipmentRef;
import com.bada.cali.mapper.StandardEquipmentRefMapper;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.repository.BusinessTripRepository;
import com.bada.cali.repository.EquipmentRefRepository;
import com.bada.cali.repository.FileInfoRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.MemberRepository;
import com.bada.cali.repository.projection.BusinessTripCalendarRow;
import com.bada.cali.repository.projection.BusinessTripListRow;
import com.bada.cali.repository.projection.ConflictEquipmentRow;
import com.bada.cali.repository.projection.MemberSelectRow;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class BusinessTripServiceImpl {

    private final BusinessTripRepository businessTripRepository;
    private final MemberRepository memberRepository;
    private final LogRepository logRepository;
    private final FileServiceImpl fileServiceImpl;
    private final FileInfoRepository fileInfoRepository;
    private final EquipmentRefRepository equipmentRefRepository;
    private final StandardEquipmentRefMapper standardEquipmentRefMapper;

    /**
     * 캘린더 기간에 해당하는 출장일정 목록 조회
     *
     * @param rangeStart 조회 시작일시 (FullCalendar 현재 뷰 시작)
     * @param rangeEnd   조회 종료일시 (FullCalendar 현재 뷰 종료)
     * @return 캘린더 이벤트 목록
     */
    @Transactional(readOnly = true)
    public List<BusinessTripDTO.CalendarEventRes> getCalendarEvents(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        List<BusinessTripCalendarRow> rows = businessTripRepository.findCalendarEvents(rangeStart, rangeEnd);

        return rows.stream()
                .map(row -> new BusinessTripDTO.CalendarEventRes(
                        row.getId(),
                        row.getType(),
                        row.getTitle(),
                        row.getStartDatetime(),
                        row.getEndDatetime(),
                        row.getCustAgent(),
                        row.getTravelerIds(),
                        row.getUpdateMemberName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 출장일정 상세 조회 (수정 모달 초기 데이터)
     *
     * @param id 출장일정 id
     * @return 상세 정보
     * @throws EntityNotFoundException id에 해당하는 데이터 없거나 삭제된 경우
     */
    @Transactional(readOnly = true)
    public BusinessTripDTO.DetailRes getDetail(Long id) {
        BusinessTrip btrip = businessTripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("출장일정을 찾을 수 없습니다. id=" + id));

        if (btrip.getDeleteDatetime() != null) {
            throw new EntityNotFoundException("삭제된 출장일정입니다. id=" + id);
        }

        // 최종수정자 이름 조회 (없으면 빈 문자열)
        String updateMemberName = "";
        if (btrip.getUpdateMemberId() != null) {
            updateMemberName = memberRepository.findById(btrip.getUpdateMemberId())
                    .map(m -> m.getName())
                    .orElse("");
        }

        // 첨부파일 건수 조회 (버튼 색상 표시용)
        long fileCnt = fileInfoRepository.countByRefTableNameAndRefTableIdAndIsVisible(
                "business_trip", id, YnType.y
        );

        return new BusinessTripDTO.DetailRes(
                btrip.getId(),
                btrip.getType(),
                btrip.getTitle(),
                btrip.getStartDatetime(),
                btrip.getEndDatetime(),
                btrip.getCustAgentId(),
                btrip.getCustAgent(),
                btrip.getCustAgentAddr(),
                btrip.getCustManager(),
                btrip.getCustManagerTel(),
                btrip.getCustManagerEmail(),
                btrip.getReportAgentId(),
                btrip.getReportAgent(),
                btrip.getReportAgentAddr(),
                btrip.getReportManager(),
                btrip.getReportManagerTel(),
                btrip.getSiteAddr(),
                btrip.getSiteManager(),
                btrip.getSiteManagerTel(),
                btrip.getSiteManagerEmail(),
                btrip.getTravelerIds(),
                btrip.getCarIds(),
                btrip.getRemark(),
                updateMemberName,
                fileCnt
        );
    }

    /**
     * 출장일정 등록
     *
     * @param req   등록 요청 DTO
     * @param files 첨부파일 목록 (없으면 null 또는 빈 리스트)
     * @param user  로그인 사용자
     * @return 생성된 출장일정 id
     */
    @Transactional
    public Long create(BusinessTripDTO.CreateReq req, List<MultipartFile> files, CustomUserDetails user) {
        Long userId = user.getId();

        BusinessTrip entity = BusinessTrip.builder()
                .title(req.title())
                .type(req.type())
                .startDatetime(req.startDatetime())
                .endDatetime(req.endDatetime())
                .custAgentId(req.custAgentId())
                .custAgent(req.custAgent())
                .custAgentAddr(req.custAgentAddr())
                .custManager(req.custManager())
                .custManagerTel(req.custManagerTel())
                .custManagerEmail(req.custManagerEmail())
                .reportAgentId(req.reportAgentId())
                .reportAgent(req.reportAgent())
                .reportAgentAddr(req.reportAgentAddr())
                .reportManager(req.reportManager())
                .reportManagerTel(req.reportManagerTel())
                .siteAddr(req.siteAddr())
                .siteManager(req.siteManager())
                .siteManagerTel(req.siteManagerTel())
                .siteManagerEmail(req.siteManagerEmail())
                .travelerIds(req.travelerIds())
                .remark(req.remark())
                .createMemberId(userId)
                .updateMemberId(userId)
                .build();

        Long savedId = businessTripRepository.save(entity).getId();

        // 표준장비 저장 (standard_equipment_ref)
        List<EquipmentDTO.UsedEquipment> equipmentDatas = req.equipmentDatas();
        if (equipmentDatas != null && !equipmentDatas.isEmpty()) {
            List<StandardEquipmentRef> equipList = new ArrayList<>();
            for (int i = 0; i < equipmentDatas.size(); i++) {
                equipList.add(standardEquipmentRefMapper.toEntityFromRecord(
                        new EquipmentDTO.UsedEquipment("business_trip", savedId, equipmentDatas.get(i).equipmentId(), i)
                ));
            }
            equipmentRefRepository.saveAll(equipList);
        }

        // 첨부파일 저장
        if (files != null && !files.isEmpty()) {
            fileServiceImpl.saveFiles("business_trip", savedId,
                    String.format("business_trip/%d/", savedId), files, userId);
        }

        // 등록 로그
        logRepository.save(Log.builder()
                .logType("i")
                .createMemberId(userId)
                .workerName(user.getName())
                .refTable("business_trip")
                .refTableId(savedId)
                .logContent(String.format("[출장일정 등록] %s - 고유번호: %d", req.title(), savedId))
                .build());

        log.info("출장일정 등록 완료 id={} userId={}", savedId, userId);
        return savedId;
    }

    /**
     * 출장일정 수정
     *
     * @param id    수정할 출장일정 id
     * @param req   수정 요청 DTO
     * @param files 추가 첨부파일 (없으면 null 또는 빈 리스트)
     * @param user  로그인 사용자
     * @throws EntityNotFoundException id에 해당하는 데이터 없거나 삭제된 경우
     */
    @Transactional
    public void update(Long id, BusinessTripDTO.UpdateReq req, List<MultipartFile> files, CustomUserDetails user) {
        BusinessTrip entity = businessTripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("출장일정을 찾을 수 없습니다. id=" + id));

        if (entity.getDeleteDatetime() != null) {
            throw new EntityNotFoundException("삭제된 출장일정입니다. id=" + id);
        }

        Long userId = user.getId();

        entity.setTitle(req.title());
        entity.setType(req.type());
        entity.setStartDatetime(req.startDatetime());
        entity.setEndDatetime(req.endDatetime());
        entity.setCustAgentId(req.custAgentId());
        entity.setCustAgent(req.custAgent());
        entity.setCustAgentAddr(req.custAgentAddr());
        entity.setCustManager(req.custManager());
        entity.setCustManagerTel(req.custManagerTel());
        entity.setCustManagerEmail(req.custManagerEmail());
        entity.setReportAgentId(req.reportAgentId());
        entity.setReportAgent(req.reportAgent());
        entity.setReportAgentAddr(req.reportAgentAddr());
        entity.setReportManager(req.reportManager());
        entity.setReportManagerTel(req.reportManagerTel());
        entity.setSiteAddr(req.siteAddr());
        entity.setSiteManager(req.siteManager());
        entity.setSiteManagerTel(req.siteManagerTel());
        entity.setSiteManagerEmail(req.siteManagerEmail());
        entity.setTravelerIds(req.travelerIds());
        entity.setRemark(req.remark());
        entity.setUpdateMemberId(userId);

        // 표준장비 갱신 (null이면 기존 유지, 빈 리스트면 전체 삭제, 값 있으면 전체 교체)
        List<EquipmentDTO.UsedEquipment> equipmentDatas = req.equipmentDatas();
        if (equipmentDatas != null) {
            // 기존 ref 데이터 전체 삭제 후 재삽입 (순서 포함)
            equipmentRefRepository.deleteUsedEquipData("business_trip", id);
            if (!equipmentDatas.isEmpty()) {
                List<StandardEquipmentRef> equipList = new ArrayList<>();
                for (int i = 0; i < equipmentDatas.size(); i++) {
                    equipList.add(standardEquipmentRefMapper.toEntityFromRecord(
                            new EquipmentDTO.UsedEquipment("business_trip", id, equipmentDatas.get(i).equipmentId(), i)
                    ));
                }
                equipmentRefRepository.saveAll(equipList);
            }
        }

        // 추가 첨부파일 저장
        if (files != null && !files.isEmpty()) {
            fileServiceImpl.saveFiles("business_trip", id,
                    String.format("business_trip/%d/", id), files, userId);
        }

        // 수정 로그
        logRepository.save(Log.builder()
                .logType("u")
                .createMemberId(userId)
                .workerName(user.getName())
                .refTable("business_trip")
                .refTableId(id)
                .logContent(String.format("[출장일정 수정] %s - 고유번호: %d", req.title(), id))
                .build());

        log.info("출장일정 수정 완료 id={} userId={}", id, userId);
    }

    /**
     * 출장자 선택용 직원 목록 조회 (selectpicker 옵션 구성)
     * - 사내 직원 전체 (is_visible = 'y', delete_datetime IS NULL)
     *
     * @return id + name 목록
     */
    @Transactional(readOnly = true)
    public List<BusinessTripDTO.MemberOption> getMemberOptions() {
        List<MemberSelectRow> rows = memberRepository.findInternalMembers();

        return rows.stream()
                .map(row -> new BusinessTripDTO.MemberOption(row.getId(), row.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 출장일정 리스트 조회 (TUI Grid 서버사이드 페이지네이션 표준 패턴)
     *
     * @param req 검색 조건 + 페이지 정보 (GetListReq extends TuiGridDTO.Request)
     * @return TuiGridDTO.ResData — contents(행 목록) + pagination(page, totalCount)
     */
    @Transactional(readOnly = true)
    public TuiGridDTO.ResData<BusinessTripListRow> getList(BusinessTripDTO.GetListReq req) {
        String keyword   = req.getKeyword();
        int    page      = req.getPage();
        int    perPage   = req.getPerPage();

        // 키워드 없으면 searchType 도 무효 (전체 조회)
        boolean hasKeyword = (keyword != null && !keyword.isBlank());
        String  st         = (hasKeyword && req.getSearchType() != null && !req.getSearchType().isBlank())
                             ? req.getSearchType() : null;
        String  kwLike     = hasKeyword ? "%" + keyword.trim() + "%" : null;

        // dateStart/dateEnd 문자열 → LocalDateTime 변환
        // 파싱 실패 시 null 처리(범위 제한 없음)
        LocalDateTime dtStart = null;
        LocalDateTime dtEnd   = null;
        try {
            if (req.getDateStart() != null && !req.getDateStart().isBlank()) {
                dtStart = LocalDate.parse(req.getDateStart()).atStartOfDay();
            }
            if (req.getDateEnd() != null && !req.getDateEnd().isBlank()) {
                dtEnd = LocalDate.parse(req.getDateEnd()).atTime(23, 59, 59);
            }
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패 — dateStart={}, dateEnd={}", req.getDateStart(), req.getDateEnd());
        }

        int offset = (page - 1) * perPage;

        List<BusinessTripListRow> rows      = businessTripRepository.findList(st, kwLike, dtStart, dtEnd, perPage, offset);
        long                      totalCount = businessTripRepository.countList(st, kwLike, dtStart, dtEnd);

        return TuiGridDTO.ResData.<BusinessTripListRow>builder()
                .contents(rows)
                .pagination(TuiGridDTO.Pagination.builder()
                        .page(page)
                        .totalCount((int) totalCount)
                        .build())
                .build();
    }

    /**
     * 출장일정 일괄 삭제 (soft delete)
     * - business_trip: delete_datetime / delete_member_id 세팅 (soft delete)
     * - standard_equipment_ref: 참조 데이터 완전 삭제 (hard delete)
     * - 이미 삭제된 건은 스킵
     *
     * @param ids  삭제할 출장일정 id 목록
     * @param user 로그인 사용자
     */
    @Transactional
    public void delete(List<Long> ids, CustomUserDetails user) {
        Long userId = user.getId();
        LocalDateTime now = LocalDateTime.now();

        for (Long id : ids) {
            BusinessTrip entity = businessTripRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("출장일정을 찾을 수 없습니다. id=" + id));

            // 이미 삭제된 건은 스킵 (멱등성 보장)
            if (entity.getDeleteDatetime() != null) {
                continue;
            }

            // 표준장비 참조 데이터 삭제 (hard delete)
            equipmentRefRepository.deleteUsedEquipData("business_trip", id);

            // 출장일정 soft delete
            entity.setDeleteDatetime(now);
            entity.setDeleteMemberId(userId);

            // 삭제 로그
            logRepository.save(Log.builder()
                    .logType("d")
                    .createMemberId(userId)
                    .workerName(user.getName())
                    .refTable("business_trip")
                    .refTableId(id)
                    .logContent(String.format("[출장일정 삭제] %s - 고유번호: %d", entity.getTitle(), id))
                    .build());
        }

        log.info("출장일정 일괄 삭제 완료 ids={} userId={}", ids, userId);
    }

    /**
     * 표준장비 중복 체크
     * - 요청한 기간과 겹치는 다른 출장일정에 이미 등록된 장비가 있으면 반환
     * - btripId: 수정 시 자기 자신 제외 (신규 등록이면 null)
     *
     * @param req 중복 체크 요청 DTO
     * @return 중복 여부 + 중복 장비 목록
     */
    @Transactional(readOnly = true)
    public BusinessTripDTO.ConflictCheckRes checkConflict(BusinessTripDTO.ConflictCheckReq req) {
        // 체크 대상 장비가 없으면 빈 응답 반환
        List<Long> equipmentIds = req.equipmentIds();
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return new BusinessTripDTO.ConflictCheckRes(false, Collections.emptyList());
        }

        // 1. 기간이 겹치는 다른 출장일정 id 목록 조회
        Set<Long> overlappingTripIds = businessTripRepository.findOverlappingTripIds(
                req.btripId(), req.startDatetime(), req.endDatetime()
        );

        if (overlappingTripIds.isEmpty()) {
            return new BusinessTripDTO.ConflictCheckRes(false, Collections.emptyList());
        }

        // 2. 그 일정들에 등록된 장비 중 요청 장비와 겹치는 것 조회
        List<ConflictEquipmentRow> conflictRows = equipmentRefRepository.findConflictEquipments(
                overlappingTripIds, equipmentIds
        );

        if (conflictRows.isEmpty()) {
            return new BusinessTripDTO.ConflictCheckRes(false, Collections.emptyList());
        }

        // 3. 응답 DTO 변환 (동일 equipmentId가 여러 출장과 겹칠 수 있으므로 첫 번째 충돌 출장만 표시)
        List<BusinessTripDTO.ConflictCheckRes.ConflictEquipmentItem> items = conflictRows.stream()
                .map(row -> new BusinessTripDTO.ConflictCheckRes.ConflictEquipmentItem(
                        row.getEquipmentId(),
                        row.getName(),
                        row.getManageNo(),
                        row.getConflictInfo()
                ))
                .collect(Collectors.toList());

        return new BusinessTripDTO.ConflictCheckRes(true, items);
    }
}
