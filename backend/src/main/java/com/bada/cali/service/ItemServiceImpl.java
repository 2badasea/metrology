package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.CreateType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.ItemDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Item;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.ItemFeeHistory;
import com.bada.cali.mapper.ItemFeeHistoryMapper;
import com.bada.cali.mapper.ItemMapper;
import com.bada.cali.repository.ItemFeeHistoryRepository;
import com.bada.cali.repository.ItemRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.projection.ItemFeeHistoryList;
import com.bada.cali.repository.projection.ItemList;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@AllArgsConstructor
public class ItemServiceImpl {
	
	private final ItemRepository itemRepository;
	private final ItemMapper itemMapper;
	private final LogRepository logRepository;
	private final ItemFeeHistoryMapper itemFeeHistoryMapper;
	private final ItemFeeHistoryRepository itemFeeHistoryRepository;
	
	// 품목관리 리스트 반환
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<ItemList> getItemList(ItemDTO.ListReq req) {
		
		int pageIndex = req.getPage() - 1;
		int perPage = req.getPerPage();
		
		PageRequest pageRequest = PageRequest.of(pageIndex, perPage);
		
		Long middleItemCodeId = req.getMiddleItemCodeId();
		if (middleItemCodeId != null && middleItemCodeId == 0L) {
			middleItemCodeId = null;
		}
		Long smallItemCodeId = req.getSmallItemCodeId();
		if (smallItemCodeId != null && smallItemCodeId == 0L) {
			smallItemCodeId = null;
		}
		
		YnType isInhousePossible = req.getIsInhousePossible(); // 빈 문자열리 넘어온 경우 DTO에 매핑하는 과정에서 null이 됨
		
		log.info("넘어오는 값 확인");
		String name = req.getName();
		if (name.isBlank()) {
			name = null;
		}
		String makeAgent = req.getMakeAgent();
		if (makeAgent.isBlank()) {
			makeAgent = null;
		}
		String format = req.getFormat();
		if (format.isBlank()) {
			format = null;
		}
		
		Page<ItemList> pageResult = itemRepository.getItemList(middleItemCodeId, smallItemCodeId, isInhousePossible, name, makeAgent, format, pageRequest);
		List<ItemList> rows = pageResult.getContent();
		
		// 페이지네이션
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		// 최종 리턴
		return TuiGridDTO.ResData.<ItemList>builder()
				.contents(rows)
				.pagination(pagination).build();
	}
	
	// 개별 품목정보 반환
	@Transactional(readOnly = true)
	public ResMessage<ItemDTO.ItemData> getItemInfo(Long id) {
		int resCode = 0;
		String resMsg = "";
		
		Item item = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("품목정보가 존재하지 않습니다."));
		resCode = 1;
		// mapstruct를 통해 entity -> record(dto)로 변환
		ItemDTO.ItemData itemData = itemMapper.toRecordFromEntity(item);
		
		return new ResMessage<>(resCode, resMsg, itemData);
	}
	
	// 품목 히스토리 확인
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<ItemFeeHistoryList> getItemFeeHistory(Long id) {
		
		// 페이징 객체도 필요없이 컨텐츠만 모두 내보낸다.
		List<ItemFeeHistoryList> itemFeeList = itemRepository.getItemFeeHistory(id);
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(0)
				.totalCount(itemFeeList.size())
				.build();
		
		return TuiGridDTO.ResData.<ItemFeeHistoryList>builder()
				.contents(itemFeeList)
				.pagination(null)
				.build();
		
	}
	
	// 품목 저장
	@Transactional
	public ResMessage<?> saveItem(ItemDTO.SaveItemData req, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		String workerName = user.getUsername();
		Long userId = user.getId();
		
		ItemDTO.ItemData itemData = req.itemData();
		Long itemId = itemData.id();
		
		String saveType = itemId == null ? "i" : "u";
		String saveTypeKr = itemId == null ? "등록" : "수정";
		
		// null인 경우 등록
		if (itemId == null) {
			// dto(record) -> entity 변환
			Item entity = itemMapper.toEntityFromRecord(itemData);
			entity.setCreateDatetime(now);
			entity.setCreateMemberId(userId);
			// 저장진행
			Item savedEntity = itemRepository.save(entity);
			itemId = savedEntity.getId();
		}
		// 수정인 경우 dirty checking
		else {
			Item originItem = itemRepository.findById(itemId).orElseThrow(() -> new EntityNotFoundException("수정하려는 품목 정보가 존재하지 않습니다."));
			
			// 넘어온 값으로 덮어씌우기
			itemMapper.updateEntityFromRecord(itemData, originItem);
			originItem.setUpdateDatetime(now);
			originItem.setUpdateMemberId(userId);
		}
		
		String logContent = String.format("[품목 %s] 품목명: %s - 고유번호: %d", saveTypeKr, itemData.name(), itemId);
		
		// 로그 남기기
		Log saveLog = Log.builder()
				.logType(saveType)
				.refTable("item")
				.refTableId(itemId)
				.createDatetime(now)
				.createMemberId(userId)
				.workerName(workerName)
				.logContent(logContent)
				.build();
		logRepository.save(saveLog);
		
		List<Long> delFeeHistoryIds = req.delHistoryIds();
		// 삭제대상 수수료 이력이 존재한다면 업데이트(is_visible = 'n')
		if (!delFeeHistoryIds.isEmpty()) {
			int resDelCount = itemRepository.deleteItemFeeHistory(itemId, YnType.n, delFeeHistoryIds, now, userId);
			
			if (resDelCount > 0) {
				String delIds = delFeeHistoryIds.stream().map(String::valueOf).collect(Collectors.joining(","));
				
				Log delFeeLog = Log.builder()
						.logType("d")
						.refTable("item_fee_history")
						.refTableId(itemId)
						.createDatetime(now)
						.createMemberId(userId)
						.workerName(workerName)
						.logContent(String.format("[품목 교정수수료 삭제] 품목 고유번호: %d, - 삭제 수수료 고유번호: %s", itemId, delIds))
						.build();
				logRepository.save(delFeeLog);
			}
		}
		
		// 교정수수료 이력을 등록/수정 한다.
		List<ItemDTO.ItemFeeData> itemFeeDataList = req.itemFeeHistoryList();
		if (!itemFeeDataList.isEmpty()) {
			for (ItemDTO.ItemFeeData itemFeeData : itemFeeDataList) {
				// id존재 유뮤로 등록/수정 판단
				Long itemFeeHistoryId = itemFeeData.id();
				if (itemFeeHistoryId == null) {
					ItemFeeHistory itemFeeHistory = itemFeeHistoryMapper.toEntityFromRecord(itemFeeData);
					itemFeeHistory.setCreateDatetime(now);
					itemFeeHistory.setCreateMemberId(userId);
					itemFeeHistory.setItemId(itemId);
					ItemFeeHistory savedEntity = itemFeeHistoryRepository.save(itemFeeHistory);
					
					// 새로 추가된 것만 남기기
					Log saveFeeLog = Log.builder()
							.logType("i")
							.workerName(workerName)
							.createMemberId(userId)
							.createDatetime(now)
							.refTable("item_fee_history")
							.refTableId(savedEntity.getId())
							.logContent(String.format("[품목 수수료 추가] 품목 고유번호: %d, - 고유번호: %d", itemId, savedEntity.getId()))
							.build();
					
				} else {
					ItemFeeHistory originEntity = itemFeeHistoryRepository.findById(itemFeeHistoryId).orElseThrow(() -> new EntityNotFoundException("수정할 교정수수료 정보가 없습니다."));
					
					itemFeeHistoryMapper.updateEntityFromRecord(itemFeeData, originEntity);
					originEntity.setUpdateDatetime(now);
					originEntity.setUpdateMemberId(userId);
				}
			}
		}    // End 교정수수료 등록/수정
		
		resCode = 1;
		resMsg = "저장되었습니다.";
		
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 품목복사
	@Transactional
	public ResMessage<?> copyItem(Long id, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		Long userId = user.getId();
		String workerName = user.getUsername();
		
		// 기존 엔티티 정보를 가져온 다음 새로운 entity에 넣어서 save()처리.
		Item targetItem = itemRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("복사할 대상 품목이 존재하지 않습니다."));
		
		Item copiedItem = new Item();
		
		String originName = targetItem.getName();
		String copiedName = originName + " [복사]";
		
		copiedItem.setFee(targetItem.getFee());
		copiedItem.setMiddleItemCodeId(targetItem.getMiddleItemCodeId());
		copiedItem.setSmallItemCodeId(targetItem.getSmallItemCodeId());
		copiedItem.setFormat(targetItem.getFormat());
		copiedItem.setCaliCycle(targetItem.getCaliCycle());
		copiedItem.setCreateType(targetItem.getCreateType());
		copiedItem.setIsInhousePossible(targetItem.getIsInhousePossible());
		copiedItem.setMakeAgent(targetItem.getMakeAgent());
		copiedItem.setMakeAgentEn(targetItem.getMakeAgentEn());
		copiedItem.setName(copiedName);
		copiedItem.setNameEn(targetItem.getNameEn());
		copiedItem.setNum(targetItem.getNum());
		copiedItem.setCreateDatetime(now);
		copiedItem.setCreateMemberId(userId);
		
		Item savedItem = itemRepository.save(copiedItem);
		Long itemId = savedItem.getId();
		String logContent = String.format("[품목 복사] 복사품목: %s - 고유번호: %d", originName, itemId);
		Log saveLog = Log.builder()
				.logType("i")
				.refTableId(itemId)
				.refTable("item")
				.createDatetime(now)
				.createMemberId(userId)
				.workerName(workerName)
				.logContent(logContent)
				.build();
		logRepository.save(saveLog);
		
		// 교정수수료 이력이 존재할 경우, 같이 복사한다.
		List<ItemFeeHistoryList> itemFeeHistoryListProjection = itemRepository.getItemFeeHistory(itemId);
		
		if (!itemFeeHistoryListProjection.isEmpty()) {
			for (ItemFeeHistoryList itemFee : itemFeeHistoryListProjection) {
				ItemFeeHistory itemFeeHistory = new ItemFeeHistory();
				itemFeeHistory.setItemId(itemId);
				itemFeeHistory.setBaseFee(itemFee.getBaseFee());
				itemFeeHistory.setBaseDate(itemFee.getBaseDate());
				itemFeeHistory.setRemark(itemFee.getRemark());
				itemFeeHistory.setCreateDatetime(now);
				itemFeeHistory.setCreateMemberId(userId);
				
				itemFeeHistoryRepository.save(itemFeeHistory);
			}
		}
		resCode = 1;
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 품목삭제
	@Transactional
	public ResMessage<?> deleteItem(List<ItemDTO.DeleteItemData> deleteItemData, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		Long userId = user.getId();
		String workerName = user.getUsername();
		
		if (!deleteItemData.isEmpty()) {
			List<Long> delItemIds = new ArrayList<>();
			List<String> delItemInfo = new ArrayList<>();
			for (ItemDTO.DeleteItemData deleteItem : deleteItemData) {
				delItemIds.add(deleteItem.id());
				delItemInfo.add(String.format("[품목 삭제] 품목명: %s - 고유번호: %d", deleteItem.name(), deleteItem.id()));
			}
			
			int resDeleteCnt = itemRepository.deleteItem(delItemIds, YnType.n, now, userId);
			if (resDeleteCnt > 0) {
				resCode = 1;
				resMsg = "삭제되었습니다.";
				// 로그를 남긴다.
				String logContent = String.join(", ", delItemInfo);
				Log deleteLog = Log.builder()
						.logType("d")
						.refTable("item")
						.logContent(logContent)
						.createDatetime(now)
						.createMemberId(userId)
						.workerName(workerName)
						.build();
				logRepository.save(deleteLog);
				
			} else {
				resCode = -1;
				resMsg = "삭제가 정상적으로 이루어지지 않았습니다.";
			}
		} else {
			resCode = -1;
			resMsg = "삭제할 품목정보가 없습니다.";
		}
		
		return new ResMessage<>(resCode, resMsg, null);
	}
	
	// 품목정보를 기준으로 중복여부를 체크한 다음 없는 경우 save 후 entity 반환
	@Transactional
	public Item findOrInsertItem(ItemDTO.FindOrInsertItemParams params, CustomUserDetails user) {
		LocalDateTime now = LocalDateTime.now();
		Long userId = user.getId();
		String workerName = user.getName();
		
		ItemDTO.ItemCheckData chkData = params.itemCheckData();    // 기기명, 제작회사, 형식 담김
		
		YnType isVisible = YnType.y;
		String name = chkData.name();
		String makeAgent = chkData.makeAgent();
		String format = chkData.format();
		
		Optional<Item> optItem = itemRepository.findFirstByIsVisibleAndNameAndMakeAgentAndFormat(isVisible, name, makeAgent, format);
		
		// 존재하는 경우, 해당 entity를 반환
		if (optItem.isPresent()) {
			return optItem.get();
		}
		// 조회 결과 존재하지 않는 경우, 새롭게 등록한다.
		else {
			Long middleItemCodeId = params.middleItemCodeId();
			if (middleItemCodeId == null || middleItemCodeId == 0) {
				middleItemCodeId = null;
			}
			Long smallItemCodeId = params.smallItemCodeId();
			if (smallItemCodeId == null || smallItemCodeId == 0) {
				smallItemCodeId = null;
			}
			Integer caliCycle = params.caliCycle();
			// 교정주기의 경우, 값이 없으면 기본 12개월 추가
			if (caliCycle == null || caliCycle == 0) {
				caliCycle = 12;
			}
			Long fee = params.fee();
			if (fee == null) {
				fee = 0L;
			}
			
			Item newItem = new Item();
			newItem.setName(name);
			newItem.setMakeAgent(makeAgent);
			newItem.setFormat(format);
			newItem.setFee(fee);
			newItem.setSmallItemCodeId(smallItemCodeId);
			newItem.setMiddleItemCodeId(middleItemCodeId);
			newItem.setCaliCycle(caliCycle);
			newItem.setIsInhousePossible(YnType.y);
			newItem.setIsVisible(isVisible);
			newItem.setCreateDatetime(now);
			newItem.setCreateMemberId(userId);
			newItem.setCreateType(CreateType.AUTO);    // 자동추가(auto) 구분
			
			Item savedItem = itemRepository.save(newItem);
			Long newItemId = savedItem.getId();
			
			// 자동으로 추가된 품목에 대한 이력을 남기고, 교정수수료가 0원이 넘으면 이력에 추가한다.
			String logContent = String.format("[품목 자동등록] 품목명: %s - 고유번호: %d", name, newItemId);
			Log newItemLog = Log.builder()
					.logType("i")
					.createMemberId(userId)
					.createDatetime(now)
					.refTableId(newItemId)
					.refTable("item")
					.workerName(workerName)
					.logContent(logContent)
					.build();
			logRepository.save(newItemLog);
			
			// 교정수수료가 0이상인 경우 품목수수료이력에도 추갛나다.
			if (newItem.getFee() > 0) {
				
				ItemFeeHistory newIfhEntity = new ItemFeeHistory();
				newIfhEntity.setBaseFee(newItem.getFee());
				newIfhEntity.setBaseDate(LocalDate.now());
				newIfhEntity.setCreateDatetime(now);
				newIfhEntity.setCreateMemberId(userId);
				newIfhEntity.setItemId(newItemId);
				newIfhEntity.setIsVisible(isVisible);
				ItemFeeHistory savedIfhEntity = itemFeeHistoryRepository.save(newIfhEntity);
				
				String ifhLogContent = String.format("[품목 수수료 추가] 품목명: %s (수수료: %d) - 고유번호: %d", name, savedIfhEntity.getBaseFee(), savedIfhEntity.getId());
				Log newIfhLog = Log.builder()
						.logType("i")
						.createMemberId(userId)
						.createDatetime(now)
						.refTableId(savedIfhEntity.getId())
						.refTable("item_fee_history")
						.workerName(workerName)
						.logContent(ifhLogContent)
						.build();
				logRepository.save(newIfhLog);
			}
			
			// 새로 추가된 Entity를 반환한다.
			return savedItem;
		}
	}
}
