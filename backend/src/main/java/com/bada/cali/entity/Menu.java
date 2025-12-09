package com.bada.cali.entity;

import com.bada.cali.common.YnType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// 업체 정보
@Entity
@Table(name = "menu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Menu {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// menu_alias : 화면에 보여줄 메뉴명
	@Column(name = "menu_alias", nullable = false, length = 100)
	private String menuAlias;
	
	// menu_code : 권한/코드용 키 (unique)
	@Column(name = "menu_code", nullable = false, length = 50, unique = true)
	private String menuCode;
	
	@Column(name = "url", length = 255)
	private String url;
	
	@Column(name = "icon_class", length = 50)
	private String iconClass;
	
	// _self / _blank
	@Column(name = "target", length = 20)
	private String target;
	
	// 메뉴 깊이
	@Column(name = "depth", nullable = false)
	private Integer depth;
	
	// 정렬순서 (동일 depth 내)
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;
	
	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(name = "is_visible", nullable = false, length = 1)
	private YnType isVisible = YnType.y;
	
	// 상위 메뉴 (최상위는 null)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Menu parent;
	
	// 하위 메뉴 목록
	@OneToMany(mappedBy = "parent")
	@OrderBy("sortOrder ASC")
	@Builder.Default
	private List<Menu> children = new ArrayList<>();
	
}
