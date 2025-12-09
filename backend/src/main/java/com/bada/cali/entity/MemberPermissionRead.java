package com.bada.cali.entity;

import jakarta.persistence.*;
import lombok.*;

// 직원별 메뉴 권한 DB
@Entity
@Table(
		name = "member_permission_read",
		// 같은 member_id, menu_id 조합으로 중복 row가 들어가지 않게 막아줌(복합 유니크 제약). insert시도 시, 오류 반환
		// '@Column(unique = ture)' 형태는 단일 컬럼 유닠에 씀
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_member_permission_read_member_menu",
						columnNames = {"member_id", "menu_id"}
				)
		}
)
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)	// 파라미터 없는 생성자를 만들되, 접근제한자 protected 설정. 기본만 쓰면 public 생성자 생성 -> 아무 코드에서 new를 이용하여 텅 빈 객체 생성가능. JPA 스펙은 entity클래스에 기본생성자 요구함. JPA가 리플렉션으로 객체를 만들 때 이 생성자를 사용하기 때문.
public class MemberPermissionRead {
	
	// 고유id
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 직원(회원)
	@ManyToOne(fetch = FetchType.LAZY)	// 엔티티 입장에서 Many(여러 개): One(하나) 관계라는 것 명시. 전형적인 FK 관계. VS OneToMany와 구분해서 파악하기. manytoone은 EAGER(즉시로딩)하는게 기본이지만, LAZY로 모든 연관관계를 정의하는 게 국룰. 정의한다고 실제 DB에 영향 없음. JPA(ORM)쪾 개념이고, 여튼JPA로 쿼리르 ㄹ나릴 대, EAGER의 경우, join해서 같이 가져와버림. LAZY는 실제로 getMenu()할 때 추가 select실행
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;
	
	// 메뉴
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menu_id", nullable = false)
	private Menu menu;
	
	// === 편의 생성 메서드 === //
	public static MemberPermissionRead of(Member member, Menu menu) {
		return MemberPermissionRead.builder()
				.member(member)
				.menu(menu)
				.build();
	}
	
}
