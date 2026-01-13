package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.*;
import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.dto.ItemDTO;
import com.bada.cali.dto.ReportDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.*;
import com.bada.cali.mapper.ReportMapper;
import com.bada.cali.mapper.StandardEquipmentRefMapper;
import com.bada.cali.repository.CaliOrderRepository;
import com.bada.cali.repository.EquipmentRefRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.ReportRepository;
import com.bada.cali.repository.projection.LastManageNoByType;
import com.bada.cali.repository.projection.LastReportNumByOrderType;
import com.bada.cali.repository.projection.OrderDetailsList;
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
import java.util.*;

@Service
@Log4j2
@AllArgsConstructor
public class ReportServiceImpl {
	
	private final ReportRepository reportRepository;
	private final CaliOrderRepository caliOrderRepository;
	private final LogRepository logRepository;
	private final ReportMapper reportMapper;
	private final ItemServiceImpl itemService;
	private final EquipmentRefRepository equipmentRefRepository;
	private final StandardEquipmentRefMapper standardEquipmentRefMapper;
	
	/**
	 * 성적서 등록
	 *
	 * @param reports
	 * @param caliOrderId
	 * @param user
	 * @return
	 */
	@Transactional
	public ResMessage<Object> addReport(List<ReportDTO.addReportReq> reports, Long caliOrderId, CustomUserDetails user) {
		
		int resCode = 0;
		String resMessage;
		
		LocalDateTime now = LocalDateTime.now();
		String workerName = user.getName();
		Long workerId = user.getId();
		
		// 접수정보
		CaliOrder orderInfo = caliOrderRepository.findById(caliOrderId).orElseThrow(() -> new EntityNotFoundException("접수정보를 알 수 없습니다."));
		ReportLang orderReportLang = orderInfo.getReportLang();    // 접수의 발행타입(KR, EN, BOTH)
		String orderNum = orderInfo.getOrderNum();        // 접수번호
		int orderYear = orderInfo.getOrderDate().getYear();    // 접수일 연도 (관리번호 조회용)
		PriorityType priorityType = orderInfo.getPriorityType();    // 우선순위
		CaliType caliType = orderInfo.getCaliType();                // 교정유형
		CaliTakeType caliTakeType = orderInfo.getCaliTakeType();    // 교정상세유형
		
		// 접수구분별 성적서번호 enumMap
		Map<OrderType, Integer> nextReportNums = new EnumMap<>(OrderType.class);
		for (OrderType t : OrderType.values()) {
			nextReportNums.put(t, 1);
		}
		// 접수구분별 성적서번호 시작 넘버링 세팅
		for (LastReportNumByOrderType p : reportRepository.findLastReportNumsByOrderType(caliOrderId)) {
			OrderType orderType = OrderType.valueOf(p.getOrderType());        // 접수구분
			String reportNum = p.getReportNum();            // 접수구분별 가장 마지막 성적서번호
			if (reportNum == null || reportNum.isBlank()) {
				continue;
				// // NOTE java에서는 substring에 음수 인덱스를 못 받기 때문에 아래와 같이 사용한다.
				// reportNum = reportNum.substring(reportNum.length() - 4);
			}
			
			// 마지막 4자리 파싱
			String suffix = reportNum.length() >= 4 ? reportNum.substring(reportNum.length() - 4) : reportNum;
			int lastNum = Integer.parseInt(suffix);
			nextReportNums.put(orderType, lastNum + 1);
		}
		
		// 접수구분별 관리번호 enumMap
		EnumMap<OrderType, Integer> nextManageNos = new EnumMap<>(OrderType.class);
		for (OrderType t : OrderType.values()) {
			nextManageNos.put(t, 1);
		}
		// 접수구분별 관리번호 시작번호 세팅
		for (LastManageNoByType m : reportRepository.findLastManageNoByOrderType(orderYear)) {
			OrderType orderType = OrderType.valueOf(m.getOrderType());
			String manageNo = m.getManageNo();
			if (manageNo == null || manageNo.isBlank()) {
				continue;
			}
			
			// 관리번호의 넘버링은 5자리
			String suffix = manageNo.length() >= 5 ? manageNo.substring(manageNo.length() - 5) : manageNo;
			int last = Integer.parseInt(suffix);
			nextManageNos.put(orderType, last + 1);
		}
		
		// 자식성적서들의 경우, list에 담았다가 부모가 저장되고 나면 일괄적으로 저장한다.
		List<Report> childrenToSave = new ArrayList<>();
		
		// 성적서번호 prefix
		Map<OrderType, String> reportNumPrefix = Map.of(OrderType.ACCREDDIT, "", OrderType.UNACCREDDIT, "B", OrderType.TESTING, "T");
		
		String manageNoPrefix = String.valueOf(orderYear).substring(2);
		
		// 반복문 진행
		for (ReportDTO.addReportReq dto : reports) {
			OrderType orderType = dto.getOrderType();        //
			
			String prefix = reportNumPrefix.get(orderType);            // 접수구분별 prefix "", "B", "T"
			int reportNumSeq = takeNext(nextReportNums, orderType);    // 성적서번호 넘버링
			int manageNoSeq = takeNext(nextManageNos, orderType);    // 관리번호 넘버링
			
			String reportNum = orderNum + "-" + prefix + String.format("%04d", reportNumSeq);    // 성적서번호
			String manageNo = "BD" + manageNoPrefix + "-" + prefix + String.format("%05d", manageNoSeq);    // 관리번호
			
			// 교정주기가 없는 경우 기본 12 주기
			Integer itemCaliCycle = dto.getItemCaliCycle();
			if (itemCaliCycle == null || itemCaliCycle == 0) {
				itemCaliCycle = 12;
			}
			
			// dto -> entity 변환하기 전 itemId를 확인한다. id가 null이거나 0인 경우, 품목조회 후 자동삽입
			Long parentItemId = dto.getItemId();
			if (parentItemId == null || parentItemId == 0) {
				
				ItemDTO.ItemCheckData itemCheckData = new ItemDTO.ItemCheckData(dto.getItemName(), dto.getItemMakeAgent(), dto.getItemFormat());
				ItemDTO.FindOrInsertItemParams findOrInsertItemParams = new ItemDTO.FindOrInsertItemParams(itemCheckData, dto.getCaliFee(), dto.getMiddleItemCodeId(), dto.getSmallItemCodeId(), dto.getItemCaliCycle());
				
				Item getItemEntity = itemService.findOrInsertItem(findOrInsertItemParams, user);
				parentItemId = getItemEntity.getId();
			}
			
			
			// entity 변환 (mapstruct)
			Report reportEntity = reportMapper.toEntity(dto);
			reportEntity.setItemId(parentItemId);
			reportEntity.setCreateDatetime(now);
			reportEntity.setCreateMemberId(workerId);
			reportEntity.setReportNum(reportNum);
			reportEntity.setManageNo(manageNo);
			reportEntity.setCaliOrderId(caliOrderId);
			reportEntity.setReportLang(orderReportLang);    // 발행타입은 기본적으로 접수를 따라간다
			reportEntity.setItemCaliCycle(itemCaliCycle);        // 교정주기
			reportEntity.setIsVisible(YnType.y);
			reportEntity.setPriorityType(priorityType);        // 긴급여부(접수정보)
			reportEntity.setCaliType(caliType);                // 접수유형(접수정보)
			reportEntity.setCaliTakeType(caliTakeType);        // 접수상세유형(접수정보)
			
			// 저장
			Report savedReport = reportRepository.save(reportEntity);
			Long parentId = savedReport.getId();        // 생성된 부모 id
			
			// 이력을 남긴다.
			Log saveLog = Log.builder()
					.createDatetime(now)
					.createMemberId(workerId)
					.workerName(workerName)
					.refTable("report")
					.refTableId(parentId)
					.logType("i")
					.logContent(String.format("[성적서 등록] 성적서 번호: %s - 고유번호: %d", reportNum, parentId))
					.build();
			logRepository.save(saveLog);
			
			// 자식 데이터가 존재하는지 확인
			if (dto.getChild() != null && !dto.getChild().isEmpty()) {
				// 자식 데이터도 반복
				for (ReportDTO.addReportReq c : dto.getChild()) {
					
					Integer childCaliCycle = c.getItemCaliCycle();
					if (childCaliCycle == null || childCaliCycle == 0) {
						childCaliCycle = 12;
					}
					
					// 자식도 마찬가지로 품목의 중복여부를 판단한다.
					Long childItemId = c.getItemId();
					if (childItemId == null || childItemId == 0) {
						ItemDTO.ItemCheckData childItemCheckData = new ItemDTO.ItemCheckData(c.getItemName(), c.getItemMakeAgent(), c.getItemFormat());
						ItemDTO.FindOrInsertItemParams childFindOrInsertItemParams = new ItemDTO.FindOrInsertItemParams(childItemCheckData, c.getCaliFee(), c.getMiddleItemCodeId(), c.getSmallItemCodeId(), c.getItemCaliCycle());
						
						Item getItemEntity = itemService.findOrInsertItem(childFindOrInsertItemParams, user);
						childItemId = getItemEntity.getId();
					}
					
					Report childEntity = reportMapper.toEntity(c);
					// 자식성적서의 경우, 성적서번호와 관리번호는 존재하지 않는다. (NULL 허용)
					childEntity.setCaliOrderId(caliOrderId);
					childEntity.setItemId(childItemId);
					childEntity.setReportLang(orderReportLang);    // 자식성적서도 접수 건의 발행타입으로 초기화
					childEntity.setCreateDatetime(now);
					childEntity.setCreateMemberId(workerId);
					childEntity.setIsVisible(YnType.y);
					childEntity.setParentScaleId(parentId);
					childEntity.setItemCaliCycle(childCaliCycle);
					
					// 리스트에 담는다. 일괄 저장 위해
					childrenToSave.add(childEntity);
				}
			}
			
			// 자식이 존재할 경우, 일괄적으로 저장
			if (!childrenToSave.isEmpty()) {
				reportRepository.saveAll(childrenToSave);
			}
		}
		
		resCode = 1;
		resMessage = "";
		return new ResMessage<>(resCode, resMessage, null);
	}
	
	
	// 숫자를 반환 후 자동으로 다시 넘버링을 올려준다.
	private int takeNext(Map<OrderType, Integer> map, OrderType type) {
		int current = map.getOrDefault(type, 1);
		map.put(type, current + 1);
		return current;
	}
	
	// 접수상세내역에 표시할 데이터를 가져온다.
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<OrderDetailsList> getOrderDetailsList(ReportDTO.GetOrderDetailsReq request) {
		// 페이징 옵션
		int pageIndex = request.getPage() - 1;
		int perPage = request.getPerPage();
		
		// 페이징 객체
		Pageable pageable = PageRequest.of(pageIndex, perPage);
		
		// 1. 접수구분, 2. 진행상태, 3. 검색타입, 4. 검색키워드 세팅
		// 1. 접수구분 (전체선택일 경우 null로 바인딩 됨
		OrderType orderType = request.getOrderType();    // 전체선택인 경우 null로 받게됨
		
		// 2. 진행상태 (Enum 타입으로 구분하지 않은 건, 성적서 관련 페이지별로 진행상태 option종류와 각 option별 해당하는 조건이 다르기 때문)
		String statusType = request.getStatusType();
		statusType = (statusType == null || statusType.isBlank()) ? null : statusType;
		
		Long middleItemCodeId = request.getMiddleItemCodeId();
		if (middleItemCodeId != null && middleItemCodeId == 0L) {
			middleItemCodeId = null;
		}
		Long smallItemCodeId = request.getSmallItemCodeId();
		if (smallItemCodeId != null && smallItemCodeId == 0L) {
			smallItemCodeId = null;
		}
		
		// 3. 검색타입
		String searchType = request.getSearchType();    // 전체선택은 all
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		
		String keyword = request.getKeyword();
		// 키워드가 혹시 null로 넘어온 경우 빈값으로 취급하여 where절을 타지 않도록 한다.
		keyword = (keyword == null) ? "" : keyword.trim();
		// 접수id
		Long caliOrderId = request.getCaliOrderId();
		
		// 프로젝션(인터페이스)를 통해서 바로 dto로 넘겨줄 데이터를 받기 때문에, Page<> 타입으로 받지 않음
		
		List<OrderDetailsList> pageResult = reportRepository.searchOrderDetails(orderType, statusType, searchType, keyword, caliOrderId, middleItemCodeId, smallItemCodeId, pageable);
		
		// NOTE 프로젝션 타입으로 바로 받기 때문에 entity -> dto 변환 과정은 생략
		
		// 페이지네이션 데이터 세팅
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(request.getPage())
				.totalCount(pageResult.size())
				.build();
		
		return TuiGridDTO.ResData.<OrderDetailsList>builder()
				.contents(pageResult)
				.pagination(pagination)
				.build();
	}
	
	// 대상 성적서들이 삭제대상으로 적합한지 확인
	@Transactional(readOnly = true)
	public ResMessage<?> isValidDelete(ReportDTO.ValidateDeleteReq request) {
		
		int code = 0;
		String message;
		
		// TODO 완료통보서, 완료통보서 품목 테이블 생성 후에 참조하고 있는 성적서id가 존재하는지 체크
		// 접수 id가 존재하는 경우, 생성된 완료통보서가 존재하는 경우 리턴
		Long caliOrderId = request.caliOrderId();
		if (caliOrderId != null && caliOrderId > 0) {
			CaliOrder orderInfo = caliOrderRepository.findById(caliOrderId).orElseThrow(() -> new EntityNotFoundException("해당 접수 건이 존재하지 않습니다."));
			// Long completionId = orderInfo.getCompletionId();
			// if (completionId != null && completionId > 0) {
			// 	code = -1;
			// }
		}
		
		// 타입별 반복문을 돌리면서 확인
		Map<String, List<Long>> validateInfo = request.validateInfo();    // record 타입이기 때문에 getXX() 아님
		if (validateInfo != null && !validateInfo.isEmpty()) {
			// map의 key를 기준으로 순회 ('ACCREDDIT' || 'UNACCREDDIT' || 'TESTING' || 'AGCY')
			for (String orderTypeStr : validateInfo.keySet()) {
				List<Long> reportIds = validateInfo.get(orderTypeStr);    // 접수타입별 id 가져오기
				
				OrderType orderType;
				ReportType reportType;
				boolean isAgcy;
				// 대행인 경우 구분
				if ("AGCY".equals(orderTypeStr)) {
					reportType = ReportType.AGCY;
					isAgcy = true;
					orderType = null;        // 대행은 접수타입을 구분하지 않는다.
				} else {
					isAgcy = false;
					reportType = ReportType.SELF;
					orderType = OrderType.valueOf(orderTypeStr);
				}
				
				// 대행의 경우엔 limit을 두지 않음. (완료유무만 판단하고, 빈 성적서번호를 허용하기 때문 - 자체성적서번호이기 때문)
				Pageable pageable = (reportType == ReportType.AGCY) ? Pageable.unpaged() : PageRequest.of(0, reportIds.size());
				
				// 대행을 제외하곤 접수타입별 id개수만큼 최신순의 성적서를 가져와서 비교한다.
				List<Report> reportList = reportRepository.getDeleteCheckReport(orderType, isAgcy, reportIds, pageable);
				
				// 자체/대행 분리 검증
				if (reportType == ReportType.AGCY) {
					// 대행이 경우, 완료유무만 확인한다.
					for (Report report : reportList) {
						ReportStatus reportStatus = report.getReportStatus();
						if (reportStatus == ReportStatus.COMPLETE) {
							code = -1;
							message = String.format("완료된 대행성적서는 삭제가 불가능합니다. (성적서번호: %s)", report.getReportNum());
							return new ResMessage<>(code, message, null);
						}
					}
					
				} else {
					// 자체성적서의 경우, 우선 id가 일치하는지 확인 후, 일치하지 않으면 return 일치하면 결재상태 점검
					List<Long> listIds = new ArrayList<>();
					for (Report report : reportList) {
						listIds.add(report.getId());    // 넘어온 id와 비교하기 위해 담는다.
						// 결재가 진행중인 게 한 건이라도 존재하면 return
						if (report.getWorkDatetime() != null || report.getApprovalDatetime() != null || report.getWorkStatus() != AppStatus.IDLE || report.getApprovalStatus() != AppStatus.IDLE) {
							code = -1;
							message = String.format("결재가 진행중인 성적서가 존재합니다. (성적서번호: %s)", report.getReportNum());
							return new ResMessage<>(code, message, null);
						}
					}
					
					Set<Long> getIds = new HashSet<>(listIds);
					Set<Long> paramIds = new HashSet<>(reportIds);
					// 일치여부 확인
					boolean isSame = getIds.size() == paramIds.size() && getIds.containsAll(paramIds);
					// 일치하지 않으면,
					if (!isSame) {
						code = -1;
						message = "각 접수구분의 마지막 성적서만 삭제가 가능합니다.";
						return new ResMessage<>(code, message, null);
					}
				}
			}
			
		} else {
			code = -1;
			message = "삭제할 정보가 올바르지 않습니다.";
			return new ResMessage<>(code, message, null);
		}
		
		// 여기까지 왔다면, 삭제하는 데 문제가 없다는 의미
		code = 1;
		message = "선택한 성적서들을 삭제하시겠습니까?";
		return new ResMessage<>(code, message, null);
	}
	
	/**
	 * 성적서 삭제 요청
	 *
	 * @param request ('deleteIds' 라는 key로 List<Long> 타입의 데이터를 받는다.
	 * @param user
	 * @return
	 */
	@Transactional
	public ResMessage<?> deleteReport(ReportDTO.DeleteReportReq request, CustomUserDetails user) {
		LocalDateTime now = LocalDateTime.now();
		String workerName = user.getName();
		Long userId = user.getId();
		List<Long> deleteIds = request.deleteIds();
		
		int code = 0;
		String msg = "";
		
		
		if (deleteIds != null && !deleteIds.isEmpty()) {
			// 대행, 자식성적서, 접수구분 상관없이 모두 가져옴
			List<Report> reportList = reportRepository.findByIdInOrParentIdInOrParentScaleIdIn(deleteIds, deleteIds, deleteIds);
			
			// 삭제된 성적서 번호
			List<String> deleteReportNums = new ArrayList<>();
			
			for (Report report : reportList) {
				
				// 자식성적서는 성적서번호, 관리번호 업데이트가 없음
				if ((report.getParentId() != null && report.getParentId() > 0) || (report.getParentScaleId() != null && report.getParentScaleId() > 0)) {
					report.setIsVisible(YnType.n);
					report.setDeleteDatetime(now);
					report.setDeleteMemberId(userId);
				}
				// 그외 부모성적서, 대행성적서의 경우엔 '성적서번호 + 'deleted' + uuid 형태로 업데이트
				else {
					String uuid = UUID.randomUUID().toString();    //
					String originReportNum = report.getReportNum();
					String originManageNo = report.getManageNo();
					String suffix = String.format("[deleted-%s", uuid);
					String newReportNum = originReportNum + suffix;
					String newManageNo = originManageNo + suffix;
					
					// dirty checking 방식으로 영속성컨텍스트로 들어온 성적서들에 대해 모두 set을 통해 update
					report.setReportNum(newReportNum);
					report.setManageNo(newManageNo);
					report.setIsVisible(YnType.n);
					report.setDeleteDatetime(now);
					report.setDeleteMemberId(userId);
					
					deleteReportNums.add(String.format("[성적서 삭제] 성적서번호: %s, - 고유 ID: %d)", originReportNum, report.getId()));
				}
			}
			
			// 로그를 남긴다.
			if (!deleteReportNums.isEmpty()) {
				String logContent = String.join(", ", deleteReportNums);
				Log deleteLog = Log.builder()
						.logType("d")
						.logContent(logContent)
						.workerName(workerName)
						.createDatetime(now)
						.createMemberId(userId)
						.refTable("report")
						.build();
				// 이력을 저장한다.
				logRepository.save(deleteLog);
				
				code = 1;
				msg = String.format("성적서 %d건이 삭제되었습니다.", deleteReportNums.size());
				
			} else {
				code = -1;
				msg = "성적서 삭제 중 문제가 발생했씁니다.";
			}
			
		} else {
			code = -1;
			msg = "삭제대상 성적서를 찾을 수 없습니다.";
		}
		return new ResMessage<>(code, msg, null);
	}
	
	/**
	 * 개별 성적서 조회
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public ReportDTO.ReportInfoRes getReportInfo(Long id) {
		
		ReportDTO.ReportInfo reportInfo = reportRepository.getReportInfo(id);
		// 자식 성적서를 조회한다.
		List<ReportDTO.ChildReportInfo> childReportInfos = reportRepository.getChildReport(id);
		return new ReportDTO.ReportInfoRes(
				reportInfo,
				childReportInfos
		);
		
	}
	
	/**
	 * 성적서 삭제 요청 (is_visible = 'n' 처리)
	 *
	 * @param id
	 * @param user
	 * @return
	 */
	@Transactional
	public ResMessage<Object> deleteById(Long id, CustomUserDetails user) {
		int resCode = 0;
		String resMsg;
		
		
		Report deleteTargetRepost = reportRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("해당 성적서가 존재하지 않습니다."));
		
		String reportNum = (deleteTargetRepost.getReportNum() == null) ? "[성적서번호 없음]" : deleteTargetRepost.getReportNum();
		
		LocalDateTime now = LocalDateTime.now();
		Long userId = user.getId();
		String workerName = user.getName();    // log에 넣을 데이터
		
		// dirty checking으로 영속성 컨텍스트 선에서 삭제처리
		deleteTargetRepost.setIsVisible(YnType.n);
		deleteTargetRepost.setDeleteDatetime(now);
		deleteTargetRepost.setDeleteMemberId(userId);
		
		String logContent = String.format("[성적서 삭제] 성적서번호: %s, 고유번호 - %d", reportNum, deleteTargetRepost.getId());
		Log deleteLog = Log.builder()
				.logType("d")
				.createMemberId(userId)
				.createDatetime(now)
				.workerName(workerName)
				.refTable("report")
				.refTableId(id)
				.logContent(logContent)
				.build();
		logRepository.save(deleteLog);
		
		resCode = 1;
		resMsg = "성적서가 삭제되었습니다.";
		
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 성적서 수정 요청
	@Transactional
	public ResMessage<Object> updateReport(ReportDTO.ReportUpdateReq req, CustomUserDetails user) {
		int resCode = 0;
		String resMessage;
		
		Long userId = user.getId();
		LocalDateTime now = LocalDateTime.now();
		String workerName = user.getName();
		// 부모데이터를 가져온다.
		Long id = req.id();
		Report updateReport = reportRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("수정할 성적서 정보를 찾는 데 실패했습니다."));
		
		// mapstruct를 통해 넘어온 값들을 그대로 영속성 컨텍스트 내 조회된 entity에 덮어씌운다.
		reportMapper.updateEntityFromDto(req, updateReport);
		updateReport.setUpdateDatetime(now);
		updateReport.setUpdateMemberId(userId);
		
		Log updateLog = Log.builder()
				.logType("u")
				.createMemberId(userId)
				.createDatetime(now)
				.workerName(workerName)
				.refTableId(id)
				.refTable("report")
				.logContent(String.format("[성적서 수정] 성적서번호: %s - 고유번호: %d", updateReport.getReportNum(), id))
				.build();
		logRepository.save(updateLog);
		
		List<EquipmentDTO.UsedEquipment> equipmentDatas = req.equipmentDatas();
		if (equipmentDatas != null && !equipmentDatas.isEmpty()) {
			// 기존에 참조하던 데이터가 있을 경우 삭제한다.
			String refTable = "report";
			equipmentRefRepository.deleteUsedEquipData(refTable, id);
			
			List<StandardEquipmentRef> saveEquipList = new ArrayList<>();
			for (EquipmentDTO.UsedEquipment equipment : equipmentDatas) {
				saveEquipList.add(standardEquipmentRefMapper.toEntityFromRecord(equipment));
			}
			equipmentRefRepository.saveAll(saveEquipList);
		}
		
		// 자식성적서도 존재하는지 확인
		List<ReportDTO.ChildReportInfo> childReportInfos = req.childReportInfos();
		// 자식성적서가 존재하는 경우, update 또는 insert
		if (childReportInfos != null && !childReportInfos.isEmpty()) {
			Long caliOrderId = updateReport.getCaliOrderId();
			ReportLang reportLang = updateReport.getReportLang();
			
			
			// 자식성적서 데이터 반복
			for (ReportDTO.ChildReportInfo childReport : childReportInfos) {
				Long childReportId = childReport.id();
				
				// id가 존재하면 update, 없으면 insert
				if (childReportId != null && childReportId > 0) {
					Report updateChildReport = reportRepository.findById(childReportId).orElseThrow(() -> new EntityNotFoundException("자식성적서가 존재하지 않습니다."));
					reportMapper.updateChildEntityFromDto(childReport, updateChildReport);
					updateChildReport.setUpdateDatetime(now);
					updateChildReport.setUpdateMemberId(userId);
					
				}
				// 신규등록인 경우
				else {
					Report newChildReport = reportMapper.insertChildEntityFromDto(childReport);
					// 부모 id를 넣어준다.
					newChildReport.setCreateDatetime(now);
					newChildReport.setCreateMemberId(userId);
					newChildReport.setParentScaleId(id);
					newChildReport.setCaliOrderId(caliOrderId);
					newChildReport.setReportLang(reportLang);
					newChildReport.setIsVisible(YnType.y);
					
					// 신규 등록
					reportRepository.save(newChildReport);
				}
				
			}
		}
		
		resCode = 1;
		resMessage = "성적서 수정에 성공했습니다.";
		
		return new ResMessage<>(resCode, resMessage, null);
	}
	
	
}
