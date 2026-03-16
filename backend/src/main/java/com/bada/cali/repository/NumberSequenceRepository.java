package com.bada.cali.repository;

import com.bada.cali.entity.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, String> {

    /**
     * 시퀀스 row를 배타락(SELECT FOR UPDATE)으로 조회.
     * 동일 트랜잭션 내에서 next_val 읽기 → 업데이트까지 원자적으로 처리하기 위해 사용.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ns FROM NumberSequence ns WHERE ns.seqKey = :seqKey")
    Optional<NumberSequence> findBySeqKeyForUpdate(@Param("seqKey") String seqKey);
}
