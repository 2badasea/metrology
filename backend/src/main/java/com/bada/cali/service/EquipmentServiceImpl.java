package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.config.NcpStorageProperties;
import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.FileInfo;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.StandardEquipment;
import com.bada.cali.mapper.StandardEquipmentMapper;
import com.bada.cali.repository.EquipmentFieldRepository;
import com.bada.cali.repository.EquipmentRepository;
import com.bada.cali.repository.FileInfoRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.projection.EquipmentListPr;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Log4j2
public class EquipmentServiceImpl {
	
	private final EquipmentRepository equipmentRepository;
	private final EquipmentFieldRepository equipmentFieldRepository;
	private final LogRepository logRepository;
	private final FileServiceImpl fileService;
	private final FileInfoRepository fileInfoRepository;
	private final StandardEquipmentMapper equipmentMapper;
	private final NcpStorageProperties storageProps; // endpoint, bucketName 등
	
	// 표준장비의 분야 데이터를 가져온다
	@Transactional(readOnly = true)
	public ResMessage<List<EquipmentDTO.EquipFieldData>> getEquipmentField(YnType isUse) {
		int resCode = 0;
		String resMsg = "";
		
		YnType isVisible = YnType.y;
		List<EquipmentDTO.EquipFieldData> resData = equipmentFieldRepository.getEquipmentField(isVisible, isUse);
		
		resCode = 1;
		return new ResMessage<>(resCode, resMsg, resData);
	}
	
	// 표준장비관리 리스트 가져오기
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<EquipmentListPr> getEquipmentList(EquipmentDTO.GetLiseReq req) {
		
		int pageIndex = req.getPage() - 1;
		int perPage = req.getPerPage();
		
		PageRequest pageRequest = PageRequest.of(pageIndex, perPage);
		
		// 검색타입, 검색키워드 및 검색필터 가공
		YnType isVisible = YnType.y;
		YnType isUse = req.getIsUse();        // 선택된 값이 없으면 NULL
		YnType isDispose = req.getIsDispose(); // 선택된 값이 없으면 NULL
		Long equipmentFieldId = req.getEquipmentFieldId();    // 분야 id
		equipmentFieldId = equipmentFieldId == 0 ? null : equipmentFieldId;
		
		String searchType = req.getSearchType();
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		String keyword = req.getKeyword();
		keyword = (keyword == null) ? "" : keyword.trim();
		
		Page<EquipmentListPr> pageResult = equipmentRepository.getEquipmentList(
				isVisible, isUse, isDispose, equipmentFieldId, searchType, keyword, pageRequest
		);
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		return TuiGridDTO.ResData.<EquipmentListPr>builder()
				.contents(pageResult.getContent())
				.pagination(pagination)
				.build();
	}
	
	// 표준장비 정보 등록/수정
	@Transactional
	public ResMessage<?> saveEquipment(EquipmentDTO.EquipmentData req, CustomUserDetails user) {
		log.info("표준장비 등록/수정 서비스 호출");
		int resCode = 0;
		String resMsg = "";
		
		Long userId = user.getId();
		LocalDateTime now = LocalDateTime.now();
		String workerName = user.getName();
		
		// 필수입력값 체크 (클라이언트단과 별개로 진행)
		String name = req.name();
		Long equipmentFieldId = req.equipmentFieldId();
		Integer dueNotifyDays = req.dueNotifyDays();
		if (name == null || name.isBlank()) {
			resCode = -1;
			resMsg = "장비명을 입력해주세요.";
		}
		if (equipmentFieldId == null || equipmentFieldId <= 0) {
			resCode = -1;
			resMsg = "분야를 선택해주세요";
		}
		if (dueNotifyDays == null || dueNotifyDays < 0) {
			resCode = -1;
			resMsg = "도래알림일은 0이상의 숫자여야 합니다.";
		}
		if (resCode == -1) {
			return new ResMessage<>(resCode, resMsg, null);
		}
		
		Long equipmentId = req.id();    // 등록(null)/ 수정 구분
		
		String logType = equipmentId == null ? "i" : "u";
		String saveTypeKr = equipmentId == null ? "등록" : "수정";
		
		// 1) 등록
		if (equipmentId == null) {
			StandardEquipment entity = equipmentMapper.toEntityFromRecord(req);
			entity.setCreateDatetime(now);
			entity.setCreateMemberId(userId);
			entity.setIsVisible(YnType.y);
			
			StandardEquipment savedEntity = equipmentRepository.save(entity);
			equipmentId = savedEntity.getId();
		}
		// 수정
		else {
			StandardEquipment originEntity = equipmentRepository.findById(equipmentId).orElseThrow(() -> new EntityNotFoundException("수정할 표준장비 정보가 존재하지 않습니다."));
			
			// record -> entity 덮어씌우기
			equipmentMapper.updateEntityFromRecord(req, originEntity);
			originEntity.setUpdateDatetime(now);
			originEntity.setUpdateMemberId(userId);
		}
		
		String refTableName = "standard_equipment";
		
		// 등록/수정에 대한 로그남기기
		Log saveLog = Log.builder()
				.logType(logType)
				.createDatetime(now)
				.createMemberId(userId)
				.workerName(workerName)
				.refTable(refTableName)
				.refTableId(equipmentId)
				.logContent(String.format("[표준장비 %s] 장비명: %s - 고유번호: %d", saveTypeKr, name, equipmentId))
				.build();
		logRepository.save(saveLog);
		
		// 표준장비 이미지 체크
		MultipartFile img = req.equipmentImgFile();
		if (img != null && !img.isEmpty()) {
			String imgDir = String.format("standard_equipment_image/%d/", equipmentId);
			
			// 기존 이미지 soft-delete (is_visible = 'n' 처리)
			int resDeleteCnt = fileInfoRepository.softDeleteVisibleByRefAndDir(
					refTableName,
					equipmentId,
					imgDir,
					YnType.n,
					now,
					userId
			);
			
			log.info("기존 것의 영향을 받은 개수 : {}", resDeleteCnt);
			
			// 새 이미지 저장 (file_info + storage)
			fileService.saveFiles(refTableName, equipmentId, imgDir, List.of(img), user);
		}
		
		// 첨부파일 체크 (기존에 존재하는 게 있다면 추가한다)
		List<MultipartFile> files = req.equipmentFiles();
		log.info("첨부 파일 확인");
		if (files != null && !files.isEmpty()) {
			log.info("여길 타니?");
			List<MultipartFile> realFiles = files.stream()
					.filter(f -> f != null && !f.isEmpty())
					.toList();
			
			if (realFiles != null && !realFiles.isEmpty()) {
				String dir = String.format("standard_equipment/%d/", equipmentId);
				log.info("saveFiles 호출");
				fileService.saveFiles(refTableName, equipmentId, dir, realFiles, user);
			}
		}
		
		resCode = 1;
		resMsg = "성공적으로 저장되었습니다.";
		
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 표준장비 데이터 가져오기
	@Transactional(readOnly = true)
	public ResMessage<EquipmentDTO.GetEquipInfos> getEquipmentInfo(Long id) {
		int resCode = 0;
		String resMsg = "";
		
		StandardEquipment entity = equipmentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("표준장비 정보를 찾지 못했습니다."));
		
		// 첨부파일 개수를 가져온다
		String refTableName = "standard_equipment";
		YnType isVisible = YnType.y;
		String dir = String.format("standard_equipment/%d/", id);
		
		// Spring Data JPA의 경우@Query 기반의 조회결과는 결과값이 없을 경우 null이 아닌 빈 리스트를 반환
		List<FileInfo> uploadFiles = fileInfoRepository.getFileInfoList(refTableName, id, dir, isVisible);
		Integer uploadFileCnt = uploadFiles.size();    // 0이상
		
		// 이미지 정보를 가져온다.
		String imageDir = String.format("standard_equipment_image/%d/", id);
		FileInfo equipImgEntity = fileInfoRepository.getFileInfo(refTableName, id, imageDir, isVisible);
		
		String equipImgPath = "";
		if (equipImgEntity != null) {
			String endpoint = storageProps.getEndpoint().replaceAll("/+$", "");
			String bucket = storageProps.getBucketName();
			String rootDir = storageProps.getRootDir();
			
			equipImgPath = endpoint + "/" + bucket + "/" + rootDir + "/" +
						   "standard_equipment_image" + "/" + id + "/" + equipImgEntity.getId() + "." + equipImgEntity.getExtension();
		}
		
		EquipmentDTO.EquipmentData equipData = equipmentMapper.toRecordFromEntity(entity);
		EquipmentDTO.GetEquipInfos resData = EquipmentDTO.GetEquipInfos.builder()
				.data(equipData)
				.uploadFileCnt(uploadFileCnt)
				.equipImgPath(equipImgPath)
				.build();
		
		resCode = 1;
		
		return new ResMessage<>(resCode, resMsg, resData);
	}
	
	// 표준장비 삭제
	@Transactional
	public ResMessage<?> deleteEquipment(EquipmentDTO.DeleteEquipmentReq req, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		Long userId = user.getId();
		String workerName = user.getName();
		
		List<Long> ids = req.deletedIds();
		if (ids.isEmpty()) {
			resCode = -1;
			resMsg = "삭제할 표준장비 정보가 없습니다.";
			return new ResMessage<>(resCode, resMsg, null);
		}
		
		YnType isVisible = YnType.n;
		int resDeleteCnt = equipmentRepository.deleteEquipment(isVisible, now, userId, ids);
		
		if (resDeleteCnt > 0) {
			resCode = 1;
			resMsg = "정상적으로 삭제되었습니다.";
			
			String deleteIds = ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
			// 다른방식 1.
			// List<String> s = ids.stream()
			// 		.map(String::valueOf)
			// 		.toList(); // Java 16+
			// String deleteIds = String.join(", ", s);
			// 다른방식 2. (null에 대한 체크)
			// String deleteIds = ids.stream()
			// 		.filter(Objects::nonNull)
			// 		.map(String::valueOf)
			// 		.collect(Collectors.joining(", "));
			
			
			// 로그를 남긴다.
			Log deleteLog = Log.builder()
					.refTable("standard_equipment")
					.refTableId(null)
					.logType("d")
					.createDatetime(now)
					.createMemberId(userId)
					.logContent(String.format("[표준장비 삭제] - 고유번호: %s", deleteIds))
					.workerName(workerName)
					.build();
			logRepository.save(deleteLog);
			
		} else {
			resCode = -1;
			resMsg = "삭제처리가 정상적으로 이루어지지 않았습니다.";
		}
		
		return new ResMessage<>(resCode, resMsg, null);
	}
}
