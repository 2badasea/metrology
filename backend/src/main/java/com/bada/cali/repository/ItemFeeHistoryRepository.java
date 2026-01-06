package com.bada.cali.repository;

import com.bada.cali.entity.ItemFeeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemFeeHistoryRepository extends JpaRepository<ItemFeeHistory, Long> {
}
