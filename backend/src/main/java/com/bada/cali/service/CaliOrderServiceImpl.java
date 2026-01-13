package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.CaliType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.AgentManager;
import com.bada.cali.entity.CaliOrder;
import com.bada.cali.entity.Log;
import com.bada.cali.mapper.CaliOrderMapper;
import com.bada.cali.repository.AgentManagerRepository;
import com.bada.cali.repository.AgentRepository;
import com.bada.cali.repository.CaliOrderRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@AllArgsConstructor
public class CaliOrderServiceImpl {
	
	private final LogRepository logRepository;
	private final CaliOrderRepository caliOrderRepository;
	private final AgentRepository agentRepository;
	private final AgentManagerRepository agentManagerRepository;
	
	private final CaliOrderMapper caliOrderMapper;
	
	// 교정접수 리스트 가져오기
	@Transactional(readOnly = true)        // 조회 전용이기에 readonly = true 명시
	public TuiGridDTO.ResData<CaliDTO.OrderRowData> getOrderList(CaliDTO.GetOrderListReq request) {
		
		int pageIndex = request.getPage() - 1;
		int pageSize = request.getPerPage();
		
		Pageable pageable = PageRequest.of(pageIndex, pageSize);    // Pageable 객체
		
		// 날짜 데이터는 문자열로 넘어오더라도 datetime으로 변경해서 리파지토리에 보내기 (JPQL에서 function 사용 방지 - DB종속적인 구조 방지)
		LocalDate startDate = null;
		LocalDate endDate = null;
		
		// 값이 존재하는지 파악 후 date타입으로 변경
		if (request.getOrderStartDate() != null && !request.getOrderStartDate().isBlank()) {
			startDate = LocalDate.parse(request.getOrderStartDate());
		}
		if (request.getOrderEndDate() != null && !request.getOrderEndDate().isBlank()) {
			endDate = LocalDate.parse(request.getOrderEndDate());
		}
		
		// 세금계산서 발행여부
		YnType isTax = null;    // 전체선택인 경우, 빈 값으로 넘어와서 null 유지 -> jpql 쿼리에서 where절 생성 방지
		String isTaxFlag = request.getIsTax();
		if ("y".equals(isTaxFlag)) {
			isTax = YnType.y;
		} else if ("n".equals(isTaxFlag)) {
			isTax = YnType.n;
		}
		
		// 교정유형
		CaliType caliType = request.getCaliType();
		// null이면(= "전체"를 null로 내려보낸 경우) 그대로 null -> where절 조건 제외
		if (caliType == null) {
			// 리파지토리에 null그대로 전달
		} else {
			caliType = switch (caliType) {
				case STANDARD, SITE -> caliType;
				default -> null;		// 혹시 그외 값들이 넘어오더라도 모두 null로 처리
			};
		}
		
		// 진행상태
		String statusType = request.getStatusType();
		statusType = switch (statusType) {
			case "" -> null;               // 빈 값으로 넘어온 경우 null 취급 -> 쿼리에서 jpql에서 where절 생성 방지
			case "wait", "complete", "hold", "cancel" -> statusType;
			default -> null;
		};
		
		// 검색타입 및 키워드 체크
		// searchType: ""(전체선택) -> null(조건제외)
		String searchType = request.getSearchType();    // 검색타입
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		searchType = switch (searchType) {
			case "all", "orderNum", "custAgent", "reportAgent", "reportAgentAddr", "remark" -> searchType;
			default -> "all";
		};
		
		String keyword = request.getKeyword();            // 검색키워드
		keyword = (keyword == null) ? "" : keyword.trim();
		
		YnType isVisible = YnType.y;        // 기본적으로 삭제된 건 노출하지 않음.
		// 분기처리 없이 데이터 가져오기
		Page<CaliOrder> pageResult = caliOrderRepository.searchOrders(isVisible, startDate, endDate, isTax, caliType, statusType, searchType, keyword, pageable);
		
		// entity -> DTO 변환
		List<CaliDTO.OrderRowData> rows = pageResult.getContent().stream().map(caliOrderMapper::toOrderDataFromEntity).toList();
		
		// 페이지네이션
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(request.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		// 최종 return
		return TuiGridDTO.ResData.<CaliDTO.OrderRowData>builder()
				.contents(rows)
				.pagination(pagination)
				.build();
	}
	
	// 교정접수 등록/수정
	@Transactional
	public Map<String, String> saveCaliOrder(CaliDTO.saveCaliOrder orderData, CustomUserDetails user) {
		// 로그인유저(세션)
		Long userId = user.getId();
		String userName = user.getName();
		
		LocalDateTime now = LocalDateTime.now();
		List<String> resMsgList = new ArrayList<>();        // 반환 메시지
		
		// 등록/수정 구분
		Long caliOrderId = orderData.getId();        //  ! 혹시나 0으로 넘	어오는 경우에도 방어 (null로 취급)
		if (caliOrderId != null && caliOrderId == 0L) {
			caliOrderId = null;
		}
		log.info("caliOrderId: {}", caliOrderId);
		
		// 등록, 수정에 따른 구분 dto -> entity로 치환한 다음 등록일 경우, 접수번호 추가해주기
		CaliOrder saveEntity;
		if (caliOrderId != null) {
			log.info("2222..???");
			saveEntity = caliOrderRepository.findById(caliOrderId).orElseThrow(() -> new EntityNotFoundException("해당 접수 건이 존재하지 않습니다."));
			// ! mapstruct mapper를 통해 영속성 엔티티 확보 & 넘어온 접수정보 기반으로 entity에 씌우기
			caliOrderMapper.updateEntityFromDto(orderData, saveEntity);
			saveEntity.setUpdateDatetime(now);
			saveEntity.setUpdateMemberId(userId);
		} else {
			log.info("2222..그럼여기?");
			saveEntity = caliOrderMapper.toSaveEntity(orderData);
			saveEntity.setCreateDatetime(now);
			saveEntity.setCreateMemberId(userId);
		}
		log.info("333333333333");
		
		// 신규 접수번호
		String newOrderNum = null;
		
		// 등록인 경우
		if (caliOrderId == null) {
			// 접수번호 생성
			newOrderNum = getOrderNum(orderData);    // 함수 호출
			saveEntity.setOrderNum(newOrderNum);
		}
		log.info("444444444444444444");
		
		// 업체 자동 생성
		// 1. 신청업체
		// ! null인지 조건을 먼저 검사해야 함. 그렇지 않을 경우, NPE가 발생할 수 있다.
		if (orderData.getCustAgentId() == null || orderData.getCustAgentId() == 0L) {
			log.info("5555");
			// 업체명, 업체주소, flag, fax, 연락처, 담당자, createType => 0인 경우엔 영문주소가 없음
			Agent agent = Agent.builder()
					.agentFlag(1)        // 신청업체 플래그
					.createType("auto")    // 자동등록
					.name(orderData.getCustAgent())
					.addr(orderData.getCustAgentAddr())
					.addrEn(orderData.getCustAgentAddrEn())
					.fax(orderData.getCustAgentFax())
					.agentTel(orderData.getCustAgentTel())
					.manager(orderData.getCustManager())
					.managerTel(orderData.getCustManagerTel())
					.managerEmail(orderData.getCustManagerEmail())
					.createMemberId(userId)
					.createDatetime(now)
					.build();
			Agent newAgent = agentRepository.save(agent);
			log.info("6666");
			Long newAgentId = newAgent.getId();        // 신규 아이디
			
			// 0이상은 당연
			if (newAgentId > 0) {
				log.info("7777");
				saveEntity.setCustAgentId(newAgentId);    // ! 접수수정인 경우에도 update 된다. (dirty check)
				log.info("8888");
				// 로그를 남긴다.
				String logContent = String.format("[신규업체(신청업체) 자동등록] 업체명: %s 고유번호: %d", newAgent.getName(), newAgentId);
				Log insertUpdateLog = Log.builder()
						.logType("i")
						.refTable("agent")
						.refTableId(newAgentId)
						.workerName(userName)
						.createDatetime(now)
						.createMemberId(userId)
						.logContent(logContent)
						.build();
				logRepository.save(insertUpdateLog);
				log.info("9999");
				
				resMsgList.add(logContent);    // 반활할 메시지에 추가
				log.info("1010");
				
				// 담당자 이름이 존재하는 경우 업체담당자 테이블에도 삽입한다.
				if (!orderData.getCustManager().isBlank()) {
					log.info("11_11");
					AgentManager agentManager = AgentManager.builder()
							.agentId(newAgentId)
							.name(orderData.getCustManager())
							.tel(orderData.getCustManagerTel())
							.email(orderData.getCustManagerEmail())
							.createMemberId(userId)
							.createDatetime(now)
							.mainYn(YnType.y)
							.build();
					agentManagerRepository.save(agentManager);
					log.info("12_12");
				}
			}
		}
		
		// 2. 성적서발행처 자동 생성
		log.info("13_13");
		// ! null인지 조건을 먼저 검사해야 함. 그렇지 않을 경우, NPE가 발생할 수 있다.
		if (orderData.getReportAgentId() == null || orderData.getReportAgentId() == 0L) {
			log.info("14_14");
			// 업체명, 업체주소, flag, fax, 연락처, 담당자, createType => 0인 경우엔 영문주소가 없음
			Agent reportAgent = Agent.builder()
					.agentFlag(4)        // 신청업체 플래그
					.createType("auto")    // 자동등록
					.name(orderData.getReportAgent())
					.addr(orderData.getReportAgentAddr())
					.addrEn(orderData.getReportAgentAddrEn())
					.manager(orderData.getReportManager())
					.managerTel(orderData.getReportManagerTel())
					.managerEmail(orderData.getReportManagerEmail())
					.createMemberId(userId)
					.createDatetime(now)
					.build();
			Agent newReportAgent = agentRepository.save(reportAgent);
			log.info("15_15");
			Long newReportAgentId = newReportAgent.getId();        // 신규 아이디
			
			// 0이상은 당연
			if (newReportAgentId > 0) {
				log.info("16_16");
				saveEntity.setReportAgentId(newReportAgentId);    // ! 접수수정인 경우에도 update 된다. (dirty check)
				
				// 로그를 남긴다.
				String logReportContent = String.format("[신규업체(성적서업체) 자동등록] 업체명: %s 고유번호: %d", newReportAgent.getName(), newReportAgentId);
				log.info("17_17");
				Log reportAgentLog = Log.builder()
						.logType("i")
						.refTable("agent")
						.refTableId(newReportAgentId)
						.workerName(userName)
						.createDatetime(now)
						.createMemberId(userId)
						.logContent(logReportContent)
						.build();
				logRepository.save(reportAgentLog);
				log.info("18_18");
				
				resMsgList.add(logReportContent);    // 반활할 메시지에 추가
				log.info("19_19");
				
				// 담당자 이름이 존재하는 경우 업체담당자 테이블에도 삽입한다.
				if (!orderData.getReportManager().isBlank()) {
					log.info("20_20");
					AgentManager reportAgentManager = AgentManager.builder()
							.agentId(newReportAgentId)
							.name(orderData.getReportManager())
							.tel(orderData.getReportManagerTel())
							.email(orderData.getReportManagerEmail())
							.createMemberId(userId)
							.createDatetime(now)
							.mainYn(YnType.y)
							.build();
					agentManagerRepository.save(reportAgentManager);
					log.info("21_21");
				}
			}
		}
		
		// 등록일 경우에만 save()메서드를 호출한다. 수정의 경우 동일 영속성 내에서 set으로 실시간 업데이트가 되기 때문
		String saveTypeKr;
		String saveLogType = (caliOrderId == null) ? "i" : "u";    // 로그타입 i:insert, u: update
		
		log.info("22_22");
		log.info("caliOrderId: {}", caliOrderId);
		if (caliOrderId == null) {
			log.info("23_23");
			CaliOrder savedCaliOrder = caliOrderRepository.save(saveEntity);
			log.info("24_24");
			caliOrderId = savedCaliOrder.getId();    // caliOrderId 변경
			saveTypeKr = "등록";
		} else {
			saveTypeKr = "수정";
		}
		log.info("25_25");
		String saveOrderNum = (newOrderNum == null) ? saveEntity.getOrderNum() : newOrderNum;
		log.info(saveOrderNum);
		log.info("26_26");
		// 접수등록에 대한 로그를 남긴다.
		String caliOrderLogContent = String.format("[교정접수 %s] 접수번호: %s, 고유번호: %d", saveTypeKr, saveOrderNum, caliOrderId);
		log.info("27_27");
		Log saveCaliOrderLog = Log.builder()
				.logType(saveLogType)
				.refTable("cali_order")
				.refTableId(caliOrderId)
				.workerName(userName)
				.createDatetime(now)
				.createMemberId(userId)
				.logContent(caliOrderLogContent)
				.build();
		logRepository.save(saveCaliOrderLog);
		log.info("28_28");
		
		String resMsg = "";
		if (!resMsgList.isEmpty()) {
			log.info("29_29");
			resMsg = String.join("<br>", resMsgList);
		}
		log.info("30_30");
		return Map.of("code", String.valueOf(caliOrderId), "msg", resMsg);
	}
	
	// 새로운 접수번호 생성 뒤 반환하기
	@Transactional(readOnly = true)
	public String getOrderNum(CaliDTO.saveCaliOrder orderData) {
		// 접수번호 규칙 BADA-YYYY-001
		String newOrderNum;
		LocalDate orderDate = LocalDate.parse(orderData.getOrderDate());    // 'yyyy-mm-dd'
		String orderYear = String.valueOf(orderDate.getYear());        // 'YYyy'
		String yy = orderYear.substring(2);                // 'yy'
		String orderPrefix = "BD" + yy;                                // 'BDyy'
		// 접수일 기준으로 그해 마지막 접수번호 + 1을 반환할 것
		CaliOrder caliOrder = caliOrderRepository.getLastOrderByYear(orderPrefix, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
		// 없는 경우, 1부터 시작
		if (caliOrder == null) {
			newOrderNum = String.format("%s-0001", orderPrefix);
		} else {
			String lasOrderNum = caliOrder.getOrderNum();
			// 뒤에 4자리 'XXXX' 반환
			String suffixNum = lasOrderNum.substring(lasOrderNum.length() - 4);
			int nextNum = Integer.parseInt(suffixNum) + 1;
			String newSuffixNum = String.format("%04d", nextNum);
			newOrderNum = String.format("%s-%s", orderPrefix, newSuffixNum);
		}
		
		return newOrderNum;
	}
	
	@Transactional(readOnly = true)
	public CaliDTO.saveCaliOrder getCaliOrderInfo(Long id) {
		
		CaliOrder caliOrder = caliOrderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("해당 접수 건이 존재하지 않습니다."));
		
		return caliOrderMapper.toDtoFromCaliOrderInfo(caliOrder);
	}
	
	// 세금계산서 발행여부 변경
	@Transactional
	public ResMessage<?> updateIsTax(CaliDTO.UpdateIsTaxReq req, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		Long userId = user.getId();
		String workerName = user.getName();
		
		Long caliOrderId = req.id();
		YnType isTax = req.isTax();
		
		int ResUpdateIsTax = caliOrderRepository.updateIsTax(caliOrderId, isTax, now, userId);
		if (ResUpdateIsTax > 0) {
			Log saveLog = Log.builder()
					.logContent(String.format("[세금계산서 발행여부 변경] 접수 ID: %d", caliOrderId))
					.createDatetime(now)
					.createMemberId(userId)
					.refTableId(caliOrderId)
					.refTable("cali_order")
					.workerName(workerName)
					.logType("u")
					.build();
			logRepository.save(saveLog);
			resCode = 1;
			resMsg = "세금계산서 발행여부가 변경되었습니다.";
		} else {
			resCode = -1;
			resMsg = "변경에 실패했습니다.";
		}
		
		return new ResMessage<>(resCode, resMsg, null);
	}
	
}
