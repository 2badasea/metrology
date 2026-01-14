package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.BasicDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Department;
import com.bada.cali.entity.MemberLevel;
import com.bada.cali.mapper.BasicMapper;
import com.bada.cali.repository.DepartmentRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.MemberLevelRepository;
import com.bada.cali.repository.projection.DepartmentListPr;
import com.bada.cali.repository.projection.MemberLevelListPr;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Log4j2
public class BasicServiceImpl {
	private final LogRepository logRepository;
	private final DepartmentRepository departmentRepository;
	private final MemberLevelRepository memberLevelRepository;
	private final BasicMapper basicMapper;
	
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
	
	// 부서/직급관리 저장
	@Transactional
	public ResMessage<?> saveBasicInfo(BasicDTO.BasicSaveDataSet req, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		Long userId = user.getId();
		LocalDateTime now = LocalDateTime.now();
		
		String targetEntity = req.type();	// 'department' | 'memberLevel'
		
		// 삭제대상이 존재하는 경우, 먼저 삭제
		List<Long> deleteIds = req.deleteIds();
		if (!deleteIds.isEmpty()) {
			// 리파지토리에서 한번에 삭제
			YnType isVisible = YnType.n;
			if ("department".equals(targetEntity)) {
				departmentRepository.deleteIds(isVisible, userId, now, deleteIds);
			} else {
				memberLevelRepository.deleteIds(isVisible, userId, now, deleteIds);
			}
			
		}
		
		// 반복문 -> id 존재 여부
		List<BasicDTO.BasicInfo> saveData = req.saveData();
		if (!saveData.isEmpty()) {
			
			// 부서관리인 경우
			if ("department".equals(targetEntity)) {
				for (BasicDTO.BasicInfo data : saveData) {
					// 등록/수정 구분
					Long id = data.id();
					// 신규등록
					if (id == null) {
						Department department = basicMapper.toDepartmentEntityFromRecord(data);
						department.createInfo(userId);
						Department savedEntity = departmentRepository.save(department);
					}
					// 수정
					else {
						// mapper 이용하기
						Department department = departmentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("부서 정보가 존재하지 않습니다."));
						department.updateInfo(data.name(), data.seq(), userId);
					}
				}
			}
			// 직급관리
			else {
				for (BasicDTO.BasicInfo data : saveData) {
					// 등록/수정 구분
					Long id = data.id();
					// 신규등록
					if (id == null) {
						MemberLevel memberLevel = basicMapper.toMemberLevelEntityFromRecord(data);
						memberLevel.createInfo(userId);
						MemberLevel savedEntity = memberLevelRepository.save(memberLevel);
					}
					// 수정
					else {
						MemberLevel memberLevel = memberLevelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("직급 정보가 존재하지 않습니다."));
						memberLevel.updateInfo(data.name(), data.seq(), userId);
					}
				}
			}
		}
		resCode = 1;
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	
}
