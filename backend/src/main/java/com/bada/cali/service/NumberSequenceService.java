package com.bada.cali.service;

import com.bada.cali.entity.NumberSequence;
import com.bada.cali.repository.NumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NumberSequenceService {

    private final NumberSequenceRepository numberSequenceRepository;

    /**
     * seqKey에 해당하는 시퀀스에서 count만큼 범위를 예약하고 시작값을 반환.
     * 반환값 ~ 반환값+count-1 까지의 번호가 호출자 트랜잭션에 독점 할당됨.
     *
     * - row가 없으면 자동 생성(next_val=1부터 시작)
     * - SELECT FOR UPDATE로 동시 접근 직렬화 보장
     * - 호출자 트랜잭션이 롤백되면 next_val도 함께 롤백 → 공백 없음
     *
     * @param seqKey  시퀀스 키 (예: "order_2026", "report_123_ACCREDDIT")
     * @param count   예약할 개수 (단건=1, 배치=N)
     * @return        예약된 범위의 시작값 (1-based)
     */
    @Transactional
    public int reserve(String seqKey, int count) {
        NumberSequence seq = numberSequenceRepository.findBySeqKeyForUpdate(seqKey)
                .orElseGet(() -> {
                    // 해당 키가 없으면 next_val=1로 신규 생성
                    NumberSequence newSeq = NumberSequence.builder()
                            .seqKey(seqKey)
                            .nextVal(1)
                            .build();
                    return numberSequenceRepository.save(newSeq);
                });

        int startVal = seq.getNextVal();       // 예약 시작값
        seq.setNextVal(startVal + count);      // count만큼 앞으로 이동
        return startVal;
    }
}