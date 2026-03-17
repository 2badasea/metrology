-- ============================================================
-- number_sequence 초기화 스크립트
-- 목적: 기존 데이터(테스트 포함)를 기준으로 next_val을 세팅하여
--       시퀀스 도입 이후 신규 채번 시 번호 충돌 방지
-- 실행 시점: number_sequence 테이블이 비어있거나 일부 row만 있는 상태에서 최초 1회 실행
-- 주의: ON DUPLICATE KEY UPDATE + GREATEST 사용 →
--       이미 세팅된 키는 기존값보다 클 때만 덮어씀 (중복 실행 안전)
-- ============================================================


-- ① 접수번호 시퀀스 초기화 (order_{year})
-- order_num 형식: BDyy-NNNN  →  마지막 '-' 이후 4자리 숫자가 seq
-- ex) BD26-0011 → next_val = 12
INSERT INTO number_sequence (seq_key, next_val)
SELECT
    CONCAT('order_', yr)                                              AS seq_key,
    MAX(CAST(SUBSTRING_INDEX(order_num, '-', -1) AS UNSIGNED)) + 1   AS next_val
FROM (
    SELECT YEAR(order_date) AS yr, order_num
    FROM cali_order
    WHERE order_num IS NOT NULL
      AND order_num REGEXP '^BD[0-9]{2}-[0-9]'   -- 정상 형식만 대상
) t
GROUP BY yr
ON DUPLICATE KEY UPDATE
    next_val = GREATEST(next_val, VALUES(next_val));


-- ② 성적서번호 시퀀스 초기화 (report_{caliOrderId}_{type})
-- report_num 형식: {orderNum}-{prefix}{seq:04d}
--   ACCREDDIT   → prefix ''  ex) BD26-0001-0003  → seq = 3
--   UNACCREDDIT → prefix 'B' ex) BD26-0001-B0002 → seq = 2  (첫 글자 'B' 제거)
--   TESTING     → prefix 'T' ex) BD26-0001-T0001 → seq = 1  (첫 글자 'T' 제거)
-- 삭제된 성적서: report_num에 '[deleted-...' 가 붙어있으므로
--   SUBSTRING_INDEX(report_num, '[', 1) 로 원본 부분만 추출 후 처리
INSERT INTO number_sequence (seq_key, next_val)
SELECT
    CONCAT('report_', cali_order_id, '_', order_type)  AS seq_key,
    MAX(
        CASE order_type
            WHEN 'ACCREDDIT' THEN
                CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(report_num, '[', 1), '-', -1) AS UNSIGNED)
            ELSE  -- UNACCREDDIT('B'), TESTING('T'): 마지막 파트 첫 글자(prefix) 제거
                CAST(SUBSTRING(SUBSTRING_INDEX(SUBSTRING_INDEX(report_num, '[', 1), '-', -1), 2) AS UNSIGNED)
        END
    ) + 1  AS next_val
FROM report
WHERE report_num IS NOT NULL
  AND order_type  IN ('ACCREDDIT', 'UNACCREDDIT', 'TESTING')
  AND parent_scale_id IS NULL   -- 분동(자식) 성적서 제외 (번호 없음)
  AND parent_id       IS NULL   -- 자식 성적서 제외
  AND report_type = 'SELF'      -- 대행 성적서는 자체 번호 체계 사용, 채번 대상 아님
GROUP BY cali_order_id, order_type
ON DUPLICATE KEY UPDATE
    next_val = GREATEST(next_val, VALUES(next_val));


-- ③ 관리번호 시퀀스 초기화 (manage_{type}_{year})
-- manage_no 형식: BD{yy}-{prefix}{seq:05d}
--   ACCREDDIT   → prefix ''  ex) BD26-00005  → seq = 5
--   UNACCREDDIT → prefix 'B' ex) BD26-B00003 → seq = 3  (첫 글자 'B' 제거)
--   TESTING     → prefix 'T' ex) BD26-T00001 → seq = 1  (첫 글자 'T' 제거)
-- year 추출: BD26-... → SUBSTRING(manage_no, 3, 2) = '26' → CONCAT('20','26') = 2026
INSERT INTO number_sequence (seq_key, next_val)
SELECT
    CONCAT('manage_', order_type, '_', yr)  AS seq_key,
    MAX(
        CASE order_type
            WHEN 'ACCREDDIT' THEN
                CAST(SUBSTRING_INDEX(manage_no_clean, '-', -1) AS UNSIGNED)
            ELSE  -- UNACCREDDIT('B'), TESTING('T')
                CAST(SUBSTRING(SUBSTRING_INDEX(manage_no_clean, '-', -1), 2) AS UNSIGNED)
        END
    ) + 1  AS next_val
FROM (
    SELECT
        order_type,
        SUBSTRING_INDEX(manage_no, '[', 1)                             AS manage_no_clean,
        CONCAT('20', SUBSTRING(SUBSTRING_INDEX(manage_no, '[', 1), 3, 2)) AS yr
    FROM report
    WHERE manage_no IS NOT NULL
      AND order_type  IN ('ACCREDDIT', 'UNACCREDDIT', 'TESTING')
      AND parent_scale_id IS NULL
      AND parent_id       IS NULL
      AND report_type = 'SELF'
) t
GROUP BY order_type, yr
ON DUPLICATE KEY UPDATE
    next_val = GREATEST(next_val, VALUES(next_val));


-- ============================================================
-- 실행 후 검증 쿼리
-- ============================================================
SELECT seq_key, next_val, update_datetime
FROM number_sequence
ORDER BY seq_key;
