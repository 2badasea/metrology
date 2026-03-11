package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.BasicDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Department;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.MemberLevel;
import com.bada.cali.mapper.BasicMapper;
import com.bada.cali.repository.DepartmentRepository;
import com.bada.cali.repository.ItemCodeRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.MemberLevelRepository;
import com.bada.cali.repository.MemberRepository;
import com.bada.cali.repository.projection.DepartmentListPr;
import com.bada.cali.repository.projection.ItemCodeList;
import com.bada.cali.repository.projection.MemberLevelListPr;
import com.bada.cali.repository.projection.MemberSelectRow;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Log4j2
public class BasicServiceImpl {
	private final LogRepository logRepository;
	private final DepartmentRepository departmentRepository;
	private final MemberLevelRepository memberLevelRepository;
	private final ItemCodeRepository itemCodeRepository;
	private final MemberRepository memberRepository;
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
		String workerName = user.getUsername();
		LocalDateTime now = LocalDateTime.now();

		String targetEntity = req.type();    // 'department' | 'memberLevel'
		String refTable = "department".equals(targetEntity) ? "department" : "member_level";
		String entityKr = "department".equals(targetEntity) ? "부서" : "직급";

		// 삭제대상이 존재하는 경우, 먼저 삭제
		List<Long> deleteIds = req.deleteIds();
		if (deleteIds != null && !deleteIds.isEmpty()) {
			// 리파지토리에서 한번에 삭제
			YnType isVisible = YnType.n;
			if ("department".equals(targetEntity)) {
				departmentRepository.deleteIds(isVisible, userId, now, deleteIds);
			} else {
				memberLevelRepository.deleteIds(isVisible, userId, now, deleteIds);
			}

			String idList = deleteIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
			Log deleteLog = Log.builder()
					.logType("d")
					.refTable(refTable)
					.refTableId(deleteIds.get(0))
					.createDatetime(now)
					.createMemberId(userId)
					.workerName(workerName)
					.logContent(String.format("[%s 삭제] 고유번호 - [%s]", entityKr, idList))
					.build();
			logRepository.save(deleteLog);
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
						Department savedDept = departmentRepository.save(department);
						Log createLog = Log.builder()
								.logType("i")
								.refTable(refTable)
								.refTableId(savedDept.getId())
								.createDatetime(now)
								.createMemberId(userId)
								.workerName(workerName)
								.logContent(String.format("[부서 등록] 부서명: %s - 고유번호: %d", data.name(), savedDept.getId()))
								.build();
						logRepository.save(createLog);
					}
					// 수정
					else {
						// mapper 이용하기
						Department department = departmentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("부서 정보가 존재하지 않습니다."));
						department.updateInfo(data.name(), data.seq(), userId);
						Log updateLog = Log.builder()
								.logType("u")
								.refTable(refTable)
								.refTableId(id)
								.createDatetime(now)
								.createMemberId(userId)
								.workerName(workerName)
								.logContent(String.format("[부서 수정] 부서명: %s - 고유번호: %d", data.name(), id))
								.build();
						logRepository.save(updateLog);
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
						MemberLevel savedLevel = memberLevelRepository.save(memberLevel);
						Log createLog = Log.builder()
								.logType("i")
								.refTable(refTable)
								.refTableId(savedLevel.getId())
								.createDatetime(now)
								.createMemberId(userId)
								.workerName(workerName)
								.logContent(String.format("[직급 등록] 직급명: %s - 고유번호: %d", data.name(), savedLevel.getId()))
								.build();
						logRepository.save(createLog);
					}
					// 수정
					else {
						MemberLevel memberLevel = memberLevelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("직급 정보가 존재하지 않습니다."));
						memberLevel.updateInfo(data.name(), data.seq(), userId);
						Log updateLog = Log.builder()
								.logType("u")
								.refTable(refTable)
								.refTableId(id)
								.createDatetime(now)
								.createMemberId(userId)
								.workerName(workerName)
								.logContent(String.format("[직급 수정] 직급명: %s - 고유번호: %d", data.name(), id))
								.build();
						logRepository.save(updateLog);
					}
				}
			}
		}
		resCode = 1;
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 부서관리 & 직급관리 정보 가져오기
	@Transactional(readOnly = true)
	public ResMessage<BasicDTO.MemberModifySetting> getBasicOptions() {
		int resCode = 0;
		String resMsg = "";
		
		List<DepartmentListPr> departmentData = departmentRepository.getDepartmentList(YnType.y);
		List<MemberLevelListPr> memberLevelData = memberLevelRepository.getMemberLevelList(YnType.y);
		List<ItemCodeList> isUseMiddleCodeData = itemCodeRepository.findAllByCodeLevelAndIsVisibleOrderByIdAsc(CodeLevel.MIDDLE, YnType.y);
		
		BasicDTO.MemberModifySetting data = new BasicDTO.MemberModifySetting(departmentData, memberLevelData, isUseMiddleCodeData);
		
		resCode = 1;
		ResMessage<BasicDTO.MemberModifySetting> result = new ResMessage<>(resCode, resMsg, data);
		
		return result;
	}
	/**
	 * 접수자 select용 사내 직원 목록 조회.
	 * 조건: agentId=0, 재직중(isActive=y), 퇴사하지 않음(leaveDate 없거나 미래), 삭제되지 않음, bada 계정 제외
	 */
	@Transactional(readOnly = true)
	public List<MemberSelectRow> getInternalMembers() {
		return memberRepository.findInternalMembers();
	}

	/**
	 * 중분류코드별 실무자/기술책임자 목록 조회.
	 * - 실무자  : authBitmask & 1 > 0 (bit1 보유)
	 * - 기술책임자: authBitmask & 6 > 0 (bit2=부 또는 bit4=정 보유, 두 유형 모두 기술책임자 select에 포함)
	 *
	 * @param middleItemCodeId 중분류코드 고유 id
	 */
	@Transactional(readOnly = true)
	public ResMessage<BasicDTO.MembersByMiddleCodeRes> getMembersByMiddleCode(Long middleItemCodeId) {
		// 실무자 권한 보유 직원 (bitmask & 1 > 0)
		List<MemberSelectRow> workers = memberRepository.findMembersByMiddleCodeAndBitmask(middleItemCodeId, 1);
		// 기술책임자 권한 보유 직원 (bitmask & 6 > 0: 부=2, 정=4 모두 포함)
		List<MemberSelectRow> approvers = memberRepository.findMembersByMiddleCodeAndBitmask(middleItemCodeId, 6);
		return new ResMessage<>(1, null, new BasicDTO.MembersByMiddleCodeRes(workers, approvers));
	}

}
