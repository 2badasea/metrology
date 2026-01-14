package com.bada.cali.service;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.DepartmentRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.MemberLevelRepository;
import com.bada.cali.repository.projection.DepartmentListPr;
import com.bada.cali.repository.projection.MemberLevelListPr;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Log4j2
public class BasicServiceImpl {
	private final LogRepository logRepository;
	private final DepartmentRepository departmentRepository;
	private final MemberLevelRepository memberLevelRepository;
	
	// 부서관리 리스트 가져오기
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<DepartmentListPr> getDepartmentList() {
		// 페이지네이션은 파라미터만 받고 사용은 안 함 (직급관리 포함)
		
		YnType isVisible = YnType.y;
		
		List<DepartmentListPr> result = departmentRepository.getDepartmentList(isVisible);
		
		// TuiGridDTO.PaginationC pagination = null;
		return TuiGridDTO.ResData.<DepartmentListPr>builder().
				pagination(null)
				.contents(result).build();
		
	}
	
	// 직급관리 리스트 가져오기
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<MemberLevelListPr> getMemberLevelList() {
		YnType isVisible = YnType.y;
		
		List<MemberLevelListPr> result = memberLevelRepository.getMemberLevelList(isVisible);
		// TuiGridDTO.PaginationC pagination = null;
		return TuiGridDTO.ResData.<MemberLevelListPr>builder().
				pagination(null)
				.contents(result).build();
	}
	
	
}
