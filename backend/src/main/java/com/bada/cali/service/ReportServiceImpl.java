package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.*;
import com.bada.cali.dto.ReportDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.CaliOrder;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.Report;
import com.bada.cali.mapper.ReportMapper;
import com.bada.cali.repository.CaliOrderRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.ReportRepository;
import com.bada.cali.repository.projection.LastManageNoByType;
import com.bada.cali.repository.projection.LastReportNumByOrderType;
import com.bada.cali.repository.projection.OrderDetailsList;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
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
	
	/**
	 * 성적서 등록
	 * @param reports
	 * @param caliOrderId
	 * @param user
	 * @return
	 */
	@Transactional
	public Boolean addReport(List<ReportDTO.addReportReq> reports, Long caliOrderId, CustomUserDetails user) {
		LocalDateTime now = LocalDateTime.now();
		String workerName = user.getName();
		Long workerId = user.getId();
		
		// 접수정보
		CaliOrder orderInfo = caliOrderRepository.findById(caliOrderId).orElseThrow(() -> new EntityNotFoundException("접수정보를 알 수 없습니다."));
		
		ReportLang orderReportLang = orderInfo.getReportLang();    // 접수의 발행타입(KR, EN, BOTH)
		String orderNum = orderInfo.getOrderNum();        // 접수번호
		int orderYear = orderInfo.getOrderDate().getYear();    // 접수일 연도 (관리번호 조회용)
		PriorityType priorityType = orderInfo.getPriorityType();
		CaliType caliType = orderInfo.getCaliType();
		CaliTakeType caliTakeType = orderInfo.getCaliTakeType();
		
		// 접수구분별 성적서번호 enumMap
		Map<OrderType, Integer> nextReportNums = new EnumMap<>(OrderType.class);
		for (OrderType t : OrderType.values()) {
			nextReportNums.put(t, 1);
		}
		// 접수구분별 성적서번호 시작 넘버링 세팅
		for (LastReportNumByOrderType p : reportRepository.findLastReportNumsByOrderType(caliOrderId)) {
			OrderType orderType = OrderType.valueOf(p.getOrderType());
			String reportNum = p.getReportNum();
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
		
		// 자식성적서들의 경우, list에 담았다가 부모가 저장되고 나면 일괄적으로 저장하한다.
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
			
			// FIX 성적서 관련 작업 이후, 품목관리, 수수료관리, 품목코드관리 등의 작업 이후에 아래 로직 수정할 것
			// TODO 품목을 조회하지 않은 경우, 해당 품목 정보를 기준으로 item 테이블에 추가한다.
			
			// entity 변환 (mapstruct)
			Report reportEntity = reportMapper.toEntity(dto);
			reportEntity.setCreateDatetime(now);
			reportEntity.setCreateMemberId(workerId);
			reportEntity.setReportNum(reportNum);
			reportEntity.setManageNo(manageNo);
			reportEntity.setCaliOrderId(caliOrderId);
			reportEntity.setReportLang(orderReportLang);    // 발행타입은 기본적으로 접수를 따라간다
			reportEntity.setItemCaliCycle(itemCaliCycle);        // 교정주기
			reportEntity.setIsVisible(YnType.y);
			reportEntity.setPriorityType(priorityType);		// 긴급여부(접수정보)
			reportEntity.setCaliType(caliType);				// 접수유형(접수정보)
			reportEntity.setCaliTakeType(caliTakeType);		// 접수상세유형(접수정보)
			
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
					
					Report childEntity = reportMapper.toEntity(c);
					// 자식성적서의 경우, 성적서번호와 관리번호는 존재하지 않는다. (NULL 허용)
					childEntity.setCaliOrderId(caliOrderId);
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
		
		return true;
	}
	
	
	// 숫자를 반환 후 자동으로 다시 넘버링을 올려준다.
	private int takeNext(Map<OrderType, Integer> map, OrderType type) {
		int current = map.getOrDefault(type, 1);
		map.put(type, current + 1);
		return current;
	}
	
	// 접수상세내역에 표시할 데이터를 가져온다.
	public TuiGridDTO.ResData<OrderDetailsList> getOrderDetailsList(ReportDTO.GetOrderDetailsReq request) {
		// 페이징 옵션
		int pageIndex = request.getPage() - 1;
		int perPage = request.getPerPage();
		
		// 페이징 객체
		Pageable pageable = PageRequest.of(pageIndex, perPage);
		
		// 1. 접수구분, 2. 진행상태, 3. 검색타입, 4. 검색키워드 세팅
		// 1. 접수구분 (전체선택일 경우 null로 바인딩 됨
		OrderType orderType = request.getOrderType();    // 전체선택인 경우 null로 받게됨
		
		// 2. 진행상태
		String statusType = request.getStatusType();
		statusType = (statusType == null || statusType.isBlank()) ? null : statusType;
		
		// 3. 검색타입
		String searchType = request.getSearchType();    // 전체선택은 all
		if (searchType == null || searchType.isBlank()) {
			searchType = null;
		}
		if (searchType != null) {
			searchType = switch (searchType) {
				case "all", "reportNum", "manageNo", "itemName", "itemMakeAgent", "itemFormat", "itemNum" -> searchType;
				default -> "all";
			};
		}
		
		String keyword = request.getKeyword();
		// 키워드가 혹시 null로 넘어온 경우 빈값으로 취급하여 where절을 타지 않도록 한다.
		keyword = (keyword == null) ? "" : keyword.trim();
		// 접수id
		Long caliOrderId = request.getCaliOrderId();
		
		List<OrderDetailsList> pageResult = reportRepository.searchOrderDetails(orderType, statusType, searchType, keyword, caliOrderId, pageable);
		// 프로젝션 타입으로 바로 받기 때문에 entity -> dto 변환 과정은 생략
		
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
		
		// 접수 id가 존재하는 경우, 생성된 완료통보서가 존재하는 경우 리턴
		Long caliOrderId = request.caliOrderId();
		if (caliOrderId != null && caliOrderId > 0) {
			CaliOrder orderInfo = caliOrderRepository.findById(caliOrderId).orElseThrow(() -> new EntityNotFoundException("해당 접수 건이 존재하지 않습니다."));
			// TODO 완료통보서, 완료통보서 품목 테이블 생성 후에 참조하고 있는 성적서id가 존재하는지 체크
			// Long completionId = orderInfo.getCompletionId();
			// if (completionId != null && completionId > 0) {
			// 	code = -1;
			// }
		}
		
		// 타입별 반복문을 돌리면서 확인
		Map<String, List<Long>> validateInfo = request.validateInfo();
		if (validateInfo != null && !validateInfo.isEmpty()) {
			for (String orderTypeStr : validateInfo.keySet()) {
				List<Long> reportIds = validateInfo.get(orderTypeStr);
				
				OrderType orderType;
				ReportType reportType;
				boolean isAgcy;
				// 대행인 경우 구분
				if ("AGCY".equals(orderTypeStr)) {
					reportType = ReportType.AGCY;
					isAgcy = true;
					orderType = null;
				} else {
					isAgcy = false;
					reportType = ReportType.SELF;
					orderType = OrderType.valueOf(orderTypeStr);
				}
				
				// 대행의 경우엔 limit을 걸지 않고, id들을 넘겨서 가져온다.
				Pageable pageable = (reportType == ReportType.AGCY) ? Pageable.unpaged() : PageRequest.of(0, reportIds.size());
				
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
						listIds.add(report.getId());	// 넘어온 id와 비교하기 위해 담는다.
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
	 * @param request ('deleteIds' 라는 key로 List<Long> 타입의 데이터를 받는다.
	 * @param user
	 * @return
	 */
	@Transactional
	public ResMessage<?> deleteReport(ReportDTO.DeleteReportReq request, CustomUserDetails user) {
		LocalDateTime now = LocalDateTime.now();
		String workerName = user.getUsername();
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
				// 그외 부모성적서, 대행성적서의 경우엔 '성적서번호 + [
				else {
					String uuid = UUID.randomUUID().toString();	//
					String originReportNum = report.getReportNum();
					String originManageNo = report.getManageNo();
					String suffix = String.format("[deleted-%s", uuid);
					String newReportNum = originReportNum + suffix;
					String newManageNo = originManageNo + suffix;
					
					report.setReportNum(newReportNum);
					report.setManageNo(newManageNo);
					report.setIsVisible(YnType.n);
					report.setDeleteDatetime(now);
					report.setDeleteMemberId(userId);
					
					deleteReportNums.add(String.format("%s(고유 ID: %d", originReportNum, report.getId()));
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
	
	
}
