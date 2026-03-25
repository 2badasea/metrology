console.log('++ cali/businessTrip.js');

$(function () {

    /* ================================================================
       출장일정 캘린더 페이지
       - FullCalendar v5 global build 사용
       - 이벤트 클릭 → 수정 모달 호출
       - 날짜 더블클릭 / '일정등록' 버튼 → 등록 모달 호출
    ================================================================ */

    const $calendarEl = document.getElementById('calendar');

    // ──────────────────────────────────────────
    // 출장자 이름 캐시 (member id → name)
    // 모달에서 select 옵션을 로드한 후 공유
    // ──────────────────────────────────────────
    let memberNameCache = {};   // { 1: '홍길동', 5: '이순신', ... }

    /**
     * travelerIds 문자열("1,5,12")을 이름 문자열로 변환
     * 캐시에 없는 id는 그냥 id로 표시
     */
    function resolveTravelerNames(travelerIds) {
        if (!travelerIds) return '';
        return travelerIds.split(',')
            .map(id => memberNameCache[id.trim()] || `#${id.trim()}`)
            .join(', ');
    }

    // ──────────────────────────────────────────
    // 직원 목록 사전 로드 (이름 캐시 구성)
    // ──────────────────────────────────────────
    async function loadMemberCache() {
        try {
            const res = await fetch('/api/admin/businessTrip/memberOptions');
            if (!res.ok) throw res;
            const json = await res.json();
            if (json.data) {
                json.data.forEach(m => {
                    memberNameCache[String(m.id)] = m.name;
                });
            }
        } catch (e) {
            console.error('직원 목록 로드 실패', e);
        }
    }

    // ──────────────────────────────────────────
    // 모달 호출 공통 함수
    // ──────────────────────────────────────────

    /**
     * 출장일정 등록 모달 호출
     * @param {string|null} dateStr - 더블클릭한 날짜 (yyyy-MM-dd), 없으면 null
     */
    async function openRegisterModal(dateStr) {
        await gModal(
            '/cali/businessTripModify',
            { id: null, defaultDate: dateStr || '' },
            {
                title: '출장일정 등록',
                size: 'xxl',
                show_close_button: true,
                show_confirm_button: true,
                confirm_button_text: '저장',
            }
        );
        // 저장 후 캘린더 이벤트 리로드
        calendar.refetchEvents();
    }

    /**
     * 출장일정 수정 모달 호출
     * @param {number} btripId - 출장일정 id
     */
    async function openModifyModal(btripId) {
        await gModal(
            '/cali/businessTripModify',
            { id: btripId },
            {
                title: '출장일정 수정',
                size: 'xxl',
                show_close_button: true,
                show_confirm_button: true,
                confirm_button_text: '저장',
                // 삭제 버튼: mr-auto로 저장/닫기 버튼의 왼쪽에 배치
                // 클릭 핸들러는 businessTripModify.js 모듈 레벨 $modal_root.on('click', '.modal-btn-delete-btrip', ...)에서 처리
                custom_btn_html_arr: [
                    '<button type="button" class="btn btn-danger btn-sm modal-btn-delete-btrip mr-auto">삭제</button>',
                ],
            }
        );
        calendar.refetchEvents();
    }

    // ──────────────────────────────────────────
    // FullCalendar 초기화
    // ──────────────────────────────────────────
    const calendar = new FullCalendar.Calendar($calendarEl, {
        locale: 'ko',
        initialView: 'dayGridMonth',
        headerToolbar: {
            left:   'prev,next today',
            center: 'title',
            right:  'listViewBtn registerBtn dayGridMonth,dayGridWeek'
        },
        buttonText: {
            today:        '오늘',
            dayGridMonth: '월',
            dayGridWeek:  '주',
        },
        customButtons: {
            // 일정등록 버튼
            registerBtn: {
                text: '일정등록',
                click: function () {
                    openRegisterModal(null);
                }
            },
            // 리스트보기 버튼 — 출장일정 리스트 페이지로 이동
            listViewBtn: {
                text: '리스트보기',
                click: function () {
                    window.location.href = '/cali/businessTripList';
                }
            }
        },

        // 일별 최대 표시 이벤트 수 (초과 시 '더보기')
        dayMaxEvents: 7,

        // 이벤트 소스: 캘린더 뷰 변경/이동 시마다 API 호출
        events: async function (fetchInfo, successCallback, failureCallback) {
            try {
                // FullCalendar startStr/endStr에는 '+09:00' 같은 timezone offset이 포함되어
                // Spring LocalDateTime이 파싱 실패하므로 앞 19자리(yyyy-MM-ddTHH:mm:ss)만 사용
                const params = new URLSearchParams({
                    rangeStart: fetchInfo.startStr.substring(0, 19),
                    rangeEnd:   fetchInfo.endStr.substring(0, 19)
                });
                const res = await fetch(`/api/admin/businessTrip/calendar?${params}`);
                if (!res.ok) throw res;
                const json = await res.json();

                // FullCalendar 이벤트 형식으로 변환
                const events = (json.data || []).map(item => ({
                    id:             item.id,
                    title:          item.title,
                    start:          item.startDatetime,
                    end:            item.endDatetime,
                    extendedProps: {
                        type:        item.type,
                        travelerIds: item.travelerIds,
                        custAgent:   item.custAgent
                    }
                }));
                successCallback(events);
            } catch (e) {
                console.error('출장일정 조회 실패', e);
                failureCallback(e);
            }
        },

        // 이벤트 커스텀 렌더링 (유형 / 제목 / 출장자 3행)
        eventContent: function (arg) {
            const props = arg.event.extendedProps;
            const typeText    = props.type || '일정';
            const titleText   = arg.event.title || '';
            const travelNames = resolveTravelerNames(props.travelerIds);

            return {
                html: `
                    <div class="fc-event-custom">
                        <div class="fc-event-type">${typeText}</div>
                        <div class="fc-event-title-custom">${titleText}</div>
                        <div class="fc-event-travelers">${travelNames}</div>
                    </div>
                `
            };
        },

        // 이벤트 클릭 → 수정 모달
        eventClick: function (info) {
            const btripId = info.event.id;
            openModifyModal(Number(btripId));
        },

        // 날짜 더블클릭 → 등록 모달
        // FullCalendar v5는 dateClick이 단일 클릭이므로, dblclick은 DOM 이벤트로 처리
        dateClick: function (info) {
            // 더블클릭 구현: 짧은 간격 내 두 번 클릭 감지
        }
    });

    calendar.render();

    // ──────────────────────────────────────────
    // 날짜 영역 더블클릭 → 등록 모달
    // FullCalendar v5는 dblclick 콜백이 없어 DOM 이벤트로 직접 처리
    // ──────────────────────────────────────────
    $('#calendar').on('dblclick', '.fc-daygrid-day', function (e) {
        // 이벤트 클릭과 중복 방지: 이벤트 요소 위에서 더블클릭한 경우 제외
        if ($(e.target).closest('.fc-event').length) return;

        const dateStr = $(this).data('date');   // yyyy-MM-dd 형식
        openRegisterModal(dateStr);
    });

    // ──────────────────────────────────────────
    // 초기 직원 목록 캐시 로드
    // ──────────────────────────────────────────
    loadMemberCache();

});
