package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.ItemCodeDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.ItemCode;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.Report;
import com.bada.cali.mapper.ItemCodeMapper;
import com.bada.cali.repository.ItemCodeRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.ReportRepository;
import com.bada.cali.repository.projection.ItemCodeList;
import com.bada.cali.repository.projection.ItemCodeRow;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bada.cali.common.enums.CodeLevel.*;

@Service
@Log4j2
@AllArgsConstructor
public class ItemCodeServiceImpl {
	
	private final ItemCodeRepository itemCodeRepository;
	private final ItemCodeMapper itemCodeMapper;
	private final LogRepository logRepository;
	private final ReportRepository reportRepository;
	
	// 분류코드관리 리스트
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<ItemCodeList> getItemCodeList(ItemCodeDTO.ItemCodeListReq req) {
		int pageIndex = req.getPage() -1;
		int perPage = req.getPerPage();
		
		Pageable pageable = PageRequest.of(pageIndex, perPage);
		
		// 페이지네이션을 위한 객체
		// codelevel과 parentId로 모두 분류한다.
		Long parentId = req.getParentId();
		if (parentId != null && parentId == 0) {
			parentId = null;
		}
		
		CodeLevel codeLevel = req.getCodeLevel();
		Page<ItemCodeList> pageResult = itemCodeRepository.searchItemCodeList(parentId, codeLevel, pageable);
		List<ItemCodeList> rows = pageResult.getContent();
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		return TuiGridDTO.ResData.<ItemCodeList>builder()
				.contents(rows)
				.pagination(pagination)
				.build();
	}
	
	// 분류코드 수정/저장 (대분류, 중분류, 소분류 저장 모두 다루기
	@Transactional
	public ResMessage<Object> saveItemCode(List<ItemCodeDTO.ItemCodeData> list, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		String wokerName = user.getName();
		Long userId = user.getId();
		
		// 순회
		for (ItemCodeDTO.ItemCodeData itemCode : list) {
			
			Long id = itemCode.id();
			String codeNum = itemCode.codeNum();
			String codeName = itemCode.codeName();
			String saveTypeKr = (id == null) ? "등록" : "수정";
			// 신규 등록
			if (id == null) {
				// record -> entity 변환 후 update
				ItemCode saveEntity = itemCodeMapper.toEntity(itemCode);
				saveEntity.setCreateDatetime(now);
				saveEntity.setCreateMemberId(userId);
				
				ItemCode savedEntity = itemCodeRepository.save(saveEntity);
				id = savedEntity.getId();
			}
			// 영속성 컨텍스트 -> dirty checking으로 업데이트
			else {
				// 조회 후, record -> entity로 mapping target후, update_datetime, member_id set()처리
				ItemCode updateEntity = itemCodeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("수정할 품목코드 정보를 찾을 수 없습니다."));
				itemCodeMapper.toEntityForUpdate(itemCode, updateEntity);
				updateEntity.setUpdateDatetime(now);
				updateEntity.setUpdateMemberId(userId);
			}
			
			// 로그를 남긴다.
			String logContent = String.format("[품목코드 %s] 품목코드: %s, 품목코드명: %s  - 고유번호 %d", saveTypeKr, codeNum, codeName, id);
			Log saveLog = Log.builder()
					.workerName(wokerName)
					.refTable("item_code")
					.refTableId(id)
					.createDatetime(now)
					.createMemberId(userId)
					.logType("i")
					.logContent(logContent)
					.build();
			
			logRepository.save(saveLog);
		}
		
		resCode = 1;
		resMsg = "저장되었습니다.";
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 삭제대ㅐ상 분류코드에 대한 검증을 한다.
	@Transactional(readOnly = true)
	public ResMessage<Map<String, String>> deleteItemCodeCheck(ItemCodeDTO.DeleteCheckReq req) {
		int resCode = 0;
		String resMsg = "";
		
		List<Long> ids = req.ids();
		CodeLevel codeLevel = req.codeLevel();
		CodeLevel childCodeLevel;
		childCodeLevel = switch (codeLevel) {
			case LARGE -> MIDDLE;
			case MIDDLE -> SMALL;
			default -> null;
		};
		Map<String, String> resMap = new HashMap<>();
		List<ItemCode> childCodeList = itemCodeRepository.findItemCodeByParentIdInAndCodeLevelAndIsVisibleOrderByParentIdAscIdAsc(ids, childCodeLevel, YnType.y);
		
		// 하위 분류코드가 존재하는 경우
		if (!childCodeList.isEmpty()) {
			List<Long> childIds = new ArrayList<>();
			for (ItemCode childItemCode : childCodeList) {
				childIds.add(childItemCode.getId());
				// 하위 분류코드를 모두 담는다.
				resMap.put(childItemCode.getCodeNum(), childItemCode.getCodeName());
			}
			
			// 자식 분류코드를 참고하고 있는 성적서를 조회한다.
			List<Report> reportList = reportRepository.findByMiddleItemCodeIdInAndIsVisible(childIds, YnType.y);
			if (reportList.isEmpty()) {
				resCode = 1;
				resMsg = "다음 분류코드도 같이 삭제됩니다.<br>삭제하시겠습니까?";
			} else {
				resCode = -1;
				resMsg = "하위 분류코드를 참조하는 성적서가 존재합니다.";
			}
			
		}
		// 하위분류코드가 존재하지 않는 경우(바로삭제가능)
		else {
			resCode = 1;
			resMsg = "삭제하시겠습니까?";
		}
		return new ResMessage<>(resCode, resMsg, resMap);
	}
	
	// 분류코드 삭제처리
	@Transactional
	public ResMessage<Object> deleteItemCode(ItemCodeDTO.DeleteCheckReq req, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		String wokerName = user.getName();
		Long userId = user.getId();
		
		// 해당 분류를 참조하는 하위 분류코드까지 모두 조회한 다음 업데이트 처리
		List<Long> ids = req.ids();
		
		// nativeQuery를 활용하여 삭제대상 id를 참조하고 있는 하위 id들도 일괄적으로 조회한 다음 삭제한다.
		List<ItemCodeRow> deleteList = itemCodeRepository.getDeleteItemCode(ids);
		if (!deleteList.isEmpty()) {
			List<Long> deleteIds = new ArrayList<>();
			List<String> deleteCodeInfo = new ArrayList<>();
			// 반복문 진행
			for (ItemCodeRow itemCodeRow : deleteList) {
				Long id = itemCodeRow.getId();
				String codeNum = itemCodeRow.getCodeNum();
				String codeName = itemCodeRow.getCodeName();
				deleteIds.add(id);
				deleteCodeInfo.add(String.format("[품목코드: %s, 품목코드명: %s, 고유번호: %d]", codeNum, codeName, id));
			}
			
			int res = itemCodeRepository.deleteItemCode(deleteIds, YnType.n, now, userId);
			// 삭제 성공 시 로그를 남긴다.
			if (res > 0) {
				resCode = 1;
				String logContent = String.join(", ", deleteCodeInfo);
				Log saveLog = Log.builder()
						.workerName(wokerName)
						.logContent(String.format("품목코드 삭제 => %s", logContent))
						.refTableId(null)
						.refTable("item_code")
						.createDatetime(now)
						.createMemberId(userId)
						.logType("d")
						.build();
				logRepository.save(saveLog);
			}
		} else {
			resCode = -1;
		}
		
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 분류코드 데이터 가져오기
	public ResMessage<List<ItemCodeList>> getItemCodeSet(CodeLevel codeLevel) {
		int resCode = 1;
		String resMsg = "";
		List<ItemCodeList> list = itemCodeRepository.findAllByCodeLevelAndIsVisibleOrderByIdAsc(codeLevel, YnType.y);
		return new ResMessage<>(resCode, resMsg, list);
	}
	
	
}
