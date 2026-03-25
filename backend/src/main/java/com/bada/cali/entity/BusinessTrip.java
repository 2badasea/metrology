package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "business_trip",
    indexes = {
        @Index(name = "idx_btrip_period", columnList = "start_datetime, end_datetime"),
        @Index(name = "idx_btrip_create", columnList = "create_member_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // 출장일정 고유 id

    // ============ 기본 정보 ============

    @Column(name = "type", length = 50)
    private String type;    // 출장유형 (추후 코드 정의 예정)

    @Column(name = "title", nullable = false, length = 200)
    private String title;   // 출장제목

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;    // 출장시작일시

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;      // 출장종료일시

    // ============ 신청업체 정보 ============

    @Column(name = "cust_agent_id")
    private Long custAgentId;           // 신청업체 agent.id (업체 조회 후 선택값)

    @Column(name = "cust_agent", length = 200)
    private String custAgent;           // 신청업체명

    @Column(name = "cust_agent_addr", length = 300)
    private String custAgentAddr;       // 신청업체 주소

    @Column(name = "cust_manager", length = 100)
    private String custManager;         // 신청업체 담당자명

    @Column(name = "cust_manager_tel", length = 50)
    private String custManagerTel;      // 신청업체 담당자 연락처

    @Column(name = "cust_manager_email", length = 125)
    private String custManagerEmail;    // 신청업체 담당자 이메일

    // ============ 성적서발행처 정보 ============

    @Column(name = "report_agent_id")
    private Long reportAgentId;         // 성적서발행처 agent.id (업체 조회 후 선택값)

    @Column(name = "report_agent", length = 200)
    private String reportAgent;         // 성적서발행처명

    @Column(name = "report_agent_addr", length = 300)
    private String reportAgentAddr;     // 성적서발행처 주소

    @Column(name = "report_manager", length = 100)
    private String reportManager;       // 성적서발행처 담당자명

    @Column(name = "report_manager_tel", length = 50)
    private String reportManagerTel;    // 성적서발행처 담당자 연락처

    // ============ 소재지 정보 ============

    @Column(name = "site_addr", length = 300)
    private String siteAddr;            // 소재지 주소

    @Column(name = "site_manager", length = 100)
    private String siteManager;         // 소재지 담당자명

    @Column(name = "site_manager_tel", length = 50)
    private String siteManagerTel;      // 소재지 담당자 연락처

    @Column(name = "site_manager_email", length = 125)
    private String siteManagerEmail;    // 소재지 담당자 이메일

    // ============ 출장자 / 출장차량 ============

    /**
     * 출장자 member.id 목록 (콤마 구분 문자열, 예: "1,5,12")
     * 수정 모달 진입 시 배열로 파싱 후 member 조회 API 호출
     */
    @Column(name = "traveler_ids", length = 500)
    private String travelerIds;

    /**
     * 출장차량 car.id 목록 (콤마 구분 문자열)
     * car 테이블 미구현 - 추후 연동 예정
     */
    @Column(name = "car_ids", length = 255)
    private String carIds;

    // ============ 기타 ============

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;  // 비고

    // ============ 감사 컬럼 ============

    @Column(name = "create_datetime", nullable = false, updatable = false)
    private LocalDateTime createDatetime;   // 등록일시

    @Column(name = "create_member_id", nullable = false, updatable = false)
    private Long createMemberId;            // 등록자 member.id

    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;   // 수정일시

    @Column(name = "update_member_id")
    private Long updateMemberId;            // 수정자 member.id

    @Column(name = "delete_datetime")
    private LocalDateTime deleteDatetime;   // 삭제일시

    @Column(name = "delete_member_id")
    private Long deleteMemberId;            // 삭제자 member.id

    // ============ JPA 생명주기 콜백 ============

    /** 등록 시 create_datetime, update_datetime 동시 세팅 */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createDatetime = now;
        this.updateDatetime = now;
        // 최초 등록 시 수정자도 등록자와 동일하게 설정 (서비스 레이어에서 세팅)
    }

    /** 수정 시 update_datetime 갱신 */
    @PreUpdate
    protected void onUpdate() {
        this.updateDatetime = LocalDateTime.now();
    }
}
