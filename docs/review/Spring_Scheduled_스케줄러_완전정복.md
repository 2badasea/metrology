# Spring `@Scheduled` — 스케줄러 완전 정복

> 계기: `AlarmServiceImpl`에서 오래된 알림을 매일 새벽 2시에 자동 삭제하기 위해 `@Scheduled(cron = "0 0 2 * * *")` 도입

---

## 1. 선행 지식 / 배경 개념

### 1-1. 스케줄링이란?

애플리케이션이 실행 중인 상태에서 **정해진 시각 또는 주기**에 특정 로직을 자동으로 실행하는 기능이다.
DB 정리 배치, 통계 집계, 캐시 갱신, 외부 API 폴링 등 다양한 용도로 쓰인다.

### 1-2. Spring의 스케줄링 구현체

Spring은 `TaskScheduler` / `ThreadPoolTaskScheduler`를 내부적으로 사용하며,
`@Scheduled` 애너테이션은 이 구현체를 메서드에 쉽게 연결하는 선언적 방식이다.

### 1-3. 스레드 모델 (중요)

기본적으로 Spring의 스케줄러는 **단일 스레드 풀(size=1)**을 사용한다.

```
스케줄러 스레드 풀 (기본 1개)
  └─ task-1: cleanupOldAlarms()   → 2:00 AM
  └─ task-2: someOtherJob()       → 2:01 AM
```

- 동시 실행이 없어 예측 가능하지만, 작업 하나가 길어지면 다음 작업이 밀림
- 병렬 처리가 필요하면 커스텀 스레드 풀 설정 필요

---

## 2. 기본 사용법

### 2-1. 활성화 (필수)

메인 클래스 또는 `@Configuration` 클래스에 `@EnableScheduling`을 추가해야 한다.

```java
@SpringBootApplication
@EnableScheduling          // ← 이것 없으면 @Scheduled가 전혀 동작하지 않음
public class CaliApplication {
    public static void main(String[] args) {
        SpringApplication.run(CaliApplication.class, args);
    }
}
```

> `@EnableScheduling`은 Spring이 `@Scheduled` 애너테이션이 붙은 메서드를 스캔해
> 스케줄러에 등록하도록 지시하는 스위치다.

### 2-2. 메서드 선언 규칙

```java
@Component   // 또는 @Service, @Repository — Spring 빈이어야 함
public class SomeScheduler {

    @Scheduled(cron = "0 0 2 * * *")
    public void myJob() {           // ✅ 반환값 없음(void), 파라미터 없음
        // ...
    }
}
```

- 반드시 `void` 반환
- 파라미터 없음
- Spring Bean(`@Component`, `@Service` 등)이어야 함

---

## 3. 실행 방식 3가지

### 3-1. cron — 특정 시각 지정

cron 표현식은 **6개 필드**로 구성된다 (표준 Unix cron의 5개 필드 + 초(Seconds) 추가).

```
@Scheduled(cron = "초 분 시 일 월 요일")

필드    범위          특수문자
초      0–59
분      0–59
시      0–23
일(月)  1–31          ?  (요일과 함께 사용 시 "지정 없음")
월      1–12          *  (모든 값)
요일    0–7 (0,7=일)  /  (간격: 0/5 = 0부터 5분마다)
                      ,  (목록: MON,WED)
                      -  (범위: 1-5 = 월~금)
                      L  (마지막: 6L = 마지막 토요일)
```

**예시:**

| cron 표현식             | 의미                        |
|-------------------------|-----------------------------|
| `0 0 2 * * *`           | 매일 오전 2시 정각           |
| `0 30 8 * * MON-FRI`    | 평일 오전 8시 30분           |
| `0 0/30 9-18 * * *`     | 오전 9시~오후 6시 30분마다   |
| `0 0 0 1 * *`           | 매월 1일 자정                |
| `0 0 0 * * 0`           | 매주 일요일 자정             |

### 3-2. fixedDelay — 이전 작업 완료 후 N밀리초 뒤 재실행

```java
@Scheduled(fixedDelay = 5000)          // 이전 실행 완료 후 5초 뒤
@Scheduled(fixedDelayString = "5000")  // 문자열로도 가능
public void taskWithDelay() { ... }
```

작업 시간이 가변적이거나 "완료 후 쉬었다 재실행"이 보장되어야 할 때 적합.

```
[실행1]-----완료---5초---[실행2]-----완료---5초---[실행3]
            ↑이 시점부터 5초 계산
```

### 3-3. fixedRate — N밀리초마다 실행 시작 (이전 완료 무관)

```java
@Scheduled(fixedRate = 5000)   // 5초마다 실행 시작
public void taskWithRate() { ... }
```

작업이 5초보다 길면 이전 작업이 끝나기 전에 다음 작업이 시작될 수 있다 (기본 단일 스레드에선 큐에 쌓임).

```
0초 [실행1]---4초 걸림---
5초 [실행2]---3초 걸림---
10초 [실행3]...
```

---

## 4. 초기 실행 지연 (initialDelay)

서버 시작 후 바로 실행되지 않고, N밀리초 후 첫 실행되도록 지연.

```java
@Scheduled(fixedRate = 10000, initialDelay = 30000)
// 서버 시작 30초 후 첫 실행, 이후 10초마다
public void delayedTask() { ... }
```

---

## 5. 이 프로젝트에서의 활용

### AlarmServiceImpl — 오래된 알림 자동 삭제

```java
// AlarmServiceImpl.java
@Scheduled(cron = "0 0 2 * * *")   // 매일 새벽 2시
@Transactional
public void cleanupOldAlarms() {
    LocalDateTime now = LocalDateTime.now();

    // 읽은 알림: 30일 경과 삭제
    int deletedRead = alarmRepository.deleteReadAlarmsBefore(now.minusDays(30));

    // 미읽음 알림: 90일 경과 삭제
    int deletedUnread = alarmRepository.deleteUnreadAlarmsBefore(now.minusDays(90));

    log.info("[AlarmCleanup] 읽음 {}건 / 미읽음 {}건 삭제 완료", deletedRead, deletedUnread);
}
```

**설계 근거:**
- 읽은 알림(30일): 이미 확인했으므로 보존 가치 낮음
- 미읽음 알림(90일): 오래된 미읽음은 더 이상 의미 없는 알림이므로 정리
- `cron = "0 0 2 * * *"`: 사용자 트래픽이 가장 낮은 새벽 2시 실행 → DB 부하 최소화
- `@Transactional`: DELETE 배치이므로 트랜잭션 묶음

**활성화 위치:**
```java
// CaliApplication.java
@SpringBootApplication
@EnableScheduling   // ← cleanupOldAlarms가 동작하려면 필수
public class CaliApplication { ... }
```

---

## 6. 주의사항

### 6-1. 기본 단일 스레드 주의

스케줄러 스레드 풀 기본 크기는 1이다.
작업 A가 10분 걸리면 작업 B는 그동안 대기한다.

여러 작업이 독립적으로 실행되어야 하면 커스텀 스레드 풀을 설정한다:

```java
@Configuration
public class SchedulerConfig implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setScheduler(Executors.newScheduledThreadPool(5));
    }
}
```

### 6-2. 다중 인스턴스(클러스터) 환경 주의

서버가 2대 이상이면 같은 스케줄이 동시에 **2번** 실행된다.
cali는 현재 단일 인스턴스 운영이므로 문제없지만, 클러스터 환경에서는:
- `@SchedulerLock` (ShedLock 라이브러리)으로 분산 락 처리
- Spring Batch + Quartz 조합 사용

### 6-3. 예외 처리

`@Scheduled` 메서드에서 예외가 발생하면 **해당 실행만 실패**하고, 다음 예약된 실행은 정상 수행된다.
단, 예외를 잡지 않으면 로그에만 남고 사라지므로 반드시 try-catch로 처리하거나 로그를 확인한다.

```java
@Scheduled(cron = "0 0 2 * * *")
public void cleanupOldAlarms() {
    try {
        // ...
    } catch (Exception e) {
        log.error("[AlarmCleanup] 오류 발생", e);
        // 필요 시 알림/모니터링 연동
    }
}
```

### 6-4. 트랜잭션

`@Scheduled`는 트랜잭션 컨텍스트가 없다.
DB 작업이 필요하면 `@Transactional`을 함께 선언하거나 트랜잭션이 있는 서비스 메서드를 호출한다.

---

## 7. 더 활용할 수 있는 방향

| 확장 방향 | 적용 예 |
|---|---|
| 성적서 기한 알림 | 교정 주기가 다가오는 기기에 대해 매일 9시에 알림 생성 |
| 임시파일 정리 | `app.temp.dir` 하위 오래된 파일 주기적 삭제 |
| 통계 집계 | 월별/주별 교정 건수를 집계 테이블에 선계산 |
| 외부 API 폴링 | cali-worker 상태 주기적 확인 (현재는 콜백 방식이므로 불필요) |
| 세션/토큰 만료 정리 | 로그인 세션 또는 임시 인증 토큰 DB에서 주기 삭제 |

---

## 8. 관련 개념 요약

| 개념 | 설명 |
|---|---|
| `@EnableScheduling` | 스캐줄러 기능 활성화 스위치 (1회 선언) |
| `@Scheduled` | 메서드에 실행 주기 부여 |
| `TaskScheduler` | Spring의 스케줄러 인터페이스 |
| `ThreadPoolTaskScheduler` | 기본 구현체 (스레드 풀 크기 조절 가능) |
| cron 6필드 | 초·분·시·일·월·요일 (표준 Unix cron + 초) |
| ShedLock | 분산 환경에서 스케줄 중복 실행 방지 라이브러리 |
