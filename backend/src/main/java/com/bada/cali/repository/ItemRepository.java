package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Item;
import com.bada.cali.repository.projection.ItemFeeHistoryList;
import com.bada.cali.repository.projection.ItemList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
	
	
	@Query("""
					select i
					from Item i
					where i.isVisible = 'y'
					and (:middleItemCodeId IS NULL OR i.middleItemCodeId = :middleItemCodeId)
					and (:smallItemCodeId IS NULL OR i.smallItemCodeId = :smallItemCodeId)
					and (:isInhousePossible IS NULL OR i.isInhousePossible = :isInhousePossible)
					and (:name IS NULL OR i.name LIKE %:name%)
					and (:makeAgent IS NULL OR i.makeAgent LIKE %:makeAgent%)
					and (:format IS NULL OR i.format LIKE %:format%)
					ORDER BY i.id ASC
			""")
	Page<ItemList> getItemList(
			@Param("middleItemCodeId") Long middleItemCodeId,
			@Param("smallItemCodeId") Long smallItemCodeId,
			@Param("isInhousePossible") YnType isInhousePossible,
			@Param("name") String name,
			@Param("makeAgent") String makeAgent,
			@Param("format") String format,
			Pageable pageable
	);
	
	// 품목관리 수수료 이력 리스트 반환
	@Query("""
			select
				ih.id as id,
				ih.itemId as itemId,
				ih.baseDate as baseDate,
				ih.baseFee as baseFee,
				ih.remark as remark
			from ItemFeeHistory  as ih
			where ih.isVisible = 'y'
						and ih.itemId = :itemId
			order by ih.baseDate DESC, ih.id DESC
			""")
	List<ItemFeeHistoryList> getItemFeeHistory(@Param("itemId") Long itemId);
	
	// TODO 굳이 @Param을 명시하는 이유가 무엇인지? 저 역할은 무엇인지? 확인 필요
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				UPDATE ItemFeeHistory AS fh
					SET fh.isVisible = :isVisible,
					fh.deleteMemberId = :deleteMemberId,
					fh.deleteDatetime = :deleteDatetime
				WHERE fh.itemId = :itemId
					AND fh.id IN (:delFeeHistoryIds)
			""")
	int deleteItemFeeHistory(
			@Param("itemId") Long itemId,
			@Param("isVisible") YnType isVisible,
			@Param("delFeeHistoryIds") List<Long> delFeeHistoryIds,
			@Param("deleteDatetime") LocalDateTime deleteDatetime,
			@Param("deleteMemberId") Long deleteMemberId
	);
	
	// 품목삭제
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				UPDATE Item AS i
					SET i.isVisible = :isVisible,
						i.deleteDatetime = :deleteDatetime,
						i.deleteMemberId = :deleteMemberId
				WHERE i.id IN (:itemIds)
			""")
	int deleteItem(
			@Param("itemIds") List<Long> itemIds,
			@Param("isVisible") YnType isVisible,
			@Param("deleteDatetime") LocalDateTime deleteDatetime,
			@Param("deleteMemberId") Long deleteMemberId
	);
	
	// 품목 중복 체크
	Optional<Item> findFirstByIsVisibleAndNameAndMakeAgentAndFormat(YnType isVisible, String name, String makeAgent, String format);
	
	
}
