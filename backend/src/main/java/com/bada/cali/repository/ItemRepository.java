package com.bada.cali.repository;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.entity.Item;
import com.bada.cali.repository.projection.ItemList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
	
	
	@Query("""
					select i
					from Item i
					where i.isVisible = 'y'
					and (:middleItemCodeId IS NULL OR i.middleItemCodeId = :middleItemCodeId)
					and (:smallItemCodeId IS NULL OR i.smallItemCodeId = :smallItemCodeId)
					and (:isInhousePossible IS NULL OR i.isInhousePossible = :isInhousePossible)
					and (
						:keyword = '' OR (
								(:searchType = 'name' AND i.name LIKE %:keyword%)
									OR (:searchType = 'makeAgent' AND i.makeAgent LIKE %:keyword%)
									OR (:searchType = 'format' AND i.format LIKE %:keyword%)
									OR (:searchType = 'num' AND i.num LIKE %:keyword%)
								OR (:searchType = 'all' AND (
										i.name        LIKE %:keyword%
					                 OR i.makeAgent   LIKE %:keyword%
					                 OR i.format LIKE %:keyword%
					                 OR i.num LIKE %:keyword%
									))
							)
					)
					ORDER BY i.id ASC
			""")
	Page<ItemList> getItemList(
			@Param("middleItemCodeId") Long middleItemCodeId,
			@Param("smallItemCodeId") Long smallItemCodeId,
			@Param("isInhousePossible")YnType isInhousePossible,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			Pageable pageable
	);
	
	
	
}
