package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.dto.ItemCodeDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.ItemCode;
import com.bada.cali.entity.Log;
import com.bada.cali.mapper.ItemCodeMapper;
import com.bada.cali.repository.ItemCodeRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.projection.ItemCodeList;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class ItemCodeServiceImpl {
	
	private final ItemCodeRepository itemCodeRepository;
	private final ItemCodeMapper itemCodeMapper;
	private final LogRepository logRepository;
	
	// 분류코드관리 리스트
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<ItemCodeList> getItemCodeList(ItemCodeDTO.ItemCodeListReq req) {
		
		// 페이지네이션을 위한 객체
		// codelevel과 parentId로 모두 분류한다.
		Long parentId = req.parentId();
		if (parentId != null && parentId == 0) {
			parentId = null;
		}
		
		CodeLevel codeLevel = req.codeLevel();
		List<ItemCodeList> pageResult = itemCodeRepository.searchItemCodeList(parentId, codeLevel);
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.totalCount(pageResult.size())
				.build();
		
		return TuiGridDTO.ResData.<ItemCodeList>builder()
				.contents(pageResult)
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
	
	
}
