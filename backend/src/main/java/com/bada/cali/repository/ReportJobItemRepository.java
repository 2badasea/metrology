package com.bada.cali.repository;

import com.bada.cali.entity.ReportJobItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportJobItemRepository extends JpaRepository<ReportJobItem, Long> {

    /** 배치 id로 소속 item 전체 조회 */
    List<ReportJobItem> findByBatchId(Long batchId);
}