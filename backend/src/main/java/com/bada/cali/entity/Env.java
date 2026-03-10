package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 회사정보 (단일 row, id=1 고정)
@Entity
@Table(name = "env")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Env {

	@Id
	@Column(name = "id")
	private Byte id;                          // 고정 row 키 (항상 1)

	@Column(name = "name", length = 100)
	private String name;                      // 회사명

	@Column(name = "name_en", length = 100)
	private String nameEn;                    // 회사명(영문)

	@Column(name = "ceo", length = 50)
	private String ceo;                       // 대표자(CEO)

	@Column(name = "tel", length = 30)
	private String tel;                       // 전화번호

	@Column(name = "fax", length = 30)
	private String fax;                       // FAX

	@Column(name = "hp", length = 30)
	private String hp;                        // 휴대폰연락처

	@Column(name = "addr", length = 200)
	private String addr;                      // 주소

	@Column(name = "addr_en", length = 200)
	private String addrEn;                    // 주소(영문)

	@Column(name = "email", length = 100)
	private String email;                     // 이메일

	@Column(name = "report_issue_addr", length = 300)
	private String reportIssueAddr;           // 성적서발행처주소

	@Column(name = "report_issue_addr_en", length = 300)
	private String reportIssueAddrEn;         // 성적서발행처주소(영문)

	@Column(name = "site_addr", length = 300)
	private String siteAddr;                  // 소재지주소

	@Column(name = "site_addr_en", length = 300)
	private String siteAddrEn;               // 소재지주소(영문)

	@Column(name = "agent_num", length = 20)
	private String agentNum;                  // 사업자등록번호

	@Column(name = "back_account", length = 200)
	private String backAccount;               // 거래은행(계좌번호)

	@Column(name = "kolas", length = 500)
	private String kolas;                     // KOLAS 이미지 경로

	@Column(name = "ilac", length = 500)
	private String ilac;                      // 아일락 이미지 경로

	@Column(name = "company", length = 500)
	private String company;                   // 사내 로고 이미지 경로

	@Column(name = "update_member_id")
	private Long updateMemberId;              // 수정자 (member.id)

	@Column(name = "update_datetime")
	private LocalDateTime updateDatetime;     // 수정일시

}