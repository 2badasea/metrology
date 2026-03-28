$(function () {
    console.log('++ alarm/alarmList.js');

    // ── $modal 설정 (표준 패턴) ──────────────────────────────────────────
    const $candidates    = $('.modal-view:not(.modal-view-applied)');
    let   $modal;
    const $bodyCandidate = $candidates.filter('.modal-body');
    if ($bodyCandidate.length) {
        $modal = $bodyCandidate.first();
    } else {
        $modal = $candidates.first();
    }

    // ── 알림 유형 한글 레이블 ────────────────────────────────────────────
    const ALARM_TYPE_LABEL = {
        WORK_COMMENT  : '개발팀 댓글',
        WORK_NOTICE   : '개발팀 공지',
        REPORT_REJECT : '성적서 반려',
        REPORT_SUBMIT : '성적서 상신',
        REPORT_REUPLOAD: '성적서 재업로드',
        BOARD_NOTICE  : '사내 공지',
    };

    // ── 미읽음 카운트 갱신 ──────────────────────────────────────────────
    async function fetchUnreadCount() {
        try {
            const res  = await fetch('/api/alarms/count');
            const json = await res.json();
            if (json && json.code === 1) {
                const count = json.data.unreadCount;
                const $badge = $('.alarmUnreadBadge', $modal);
                if (count > 0) {
                    $badge.text(count > 99 ? '99+' : count).removeClass('d-none');
                } else {
                    $badge.addClass('d-none');
                }
                // 상위 topbar 벨 뱃지 동기화
                if (typeof window.updateAlarmBadge === 'function') {
                    window.updateAlarmBadge(count);
                }
            }
        } catch (e) { /* 무시 */ }
    }

    // ── gGrid data_source ────────────────────────────────────────────────
    $modal.data_source = {
        api: {
            readData: {
                url: '/api/alarms',
                method: 'GET',
                serializer: (gridParam) => {
                    // alarmType 필터 값 주입
                    gridParam.alarmType = $('.alarmTypeFilter', $modal).val();
                    return $.param(gridParam);
                },
            },
        },
    };

    // ── Toast UI Grid 정의 ───────────────────────────────────────────────
    // IsReadRenderer는 gridClass.js에 전역 선언되어 있음
    $modal.grid = gGrid('.alarmGrid', {
        columns: [
            {
                header: '유형',
                name: 'alarmType',
                width: 110,
                align: 'center',
                formatter: ({ value }) => ALARM_TYPE_LABEL[value] ?? value ?? '',
            },
            {
                header: '내용',
                name: 'content',
                minWidth: 200,
                // 미읽음 행은 굵게
                renderer: {
                    type: function (props) {
                        const el = document.createElement('span');
                        el.style.cssText = props.row.isRead === 'n'
                            ? 'font-weight:600;'
                            : '';
                        el.textContent = props.value;
                        this.el = el;
                        return this;
                    },
                    styles: {},
                },
            },
            {
                header: '발신',
                name: 'senderName',
                width: 80,
                align: 'center',
                formatter: ({ value }) => value || '시스템',
            },
            {
                header: '상태',
                name: 'isRead',
                width: 75,
                align: 'center',
                renderer: { type: IsReadRenderer },
            },
            {
                header: '생성일시',
                name: 'createDatetime',
                width: 130,
                align: 'center',
                formatter: ({ value }) => {
                    if (!value) return '';
                    const d = new Date(value);
                    if (isNaN(d)) return value;
                    const pad = n => String(n).padStart(2, '0');
                    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
                },
            },
        ],
        pageOptions: {
            useClient: false,  // 서버 페이징
            perPage: 20,
        },
        bodyHeight: 360,
        minBodyHeight: 80,
        data: $modal.data_source,
    });

    // ── 그리드 행 클릭: 개별 읽음 처리 ─────────────────────────────────
    $modal.grid.on('click', async ({ rowKey }) => {
        const row = $modal.grid.getRow(rowKey);
        if (!row || row.isRead === 'y') return;

        try {
            const res  = await fetch(`/api/alarms/${row.id}/read`, { method: 'PATCH' });
            const json = await res.json();
            if (json && json.code === 1) {
                // 해당 행만 읽음 상태로 갱신 (그리드 전체 재로드 없이)
                $modal.grid.setValue(rowKey, 'isRead', 'y');
                fetchUnreadCount();
            }
        } catch (e) {
            gErrorHandler('읽음 처리 중 오류가 발생했습니다.');
        }
    });

    // ── 전체 읽음 버튼 ──────────────────────────────────────────────────
    $('.btnReadAll', $modal).on('click', async function () {
        try {
            const res  = await fetch('/api/alarms/readAll', { method: 'PATCH' });
            const json = await res.json();
            if (json && json.code === 1) {
                // 그리드 데이터 전체 재로드
                $modal.grid.reloadData();
                fetchUnreadCount();
            }
        } catch (e) {
            gErrorHandler('전체 읽음 처리 중 오류가 발생했습니다.');
        }
    });

    // ── 유형 필터 변경 → 그리드 1페이지로 재조회 ───────────────────────
    $('.alarmTypeFilter', $modal).on('change', function () {
        $modal.grid.getPagination().movePageTo(1);
    });

    // ── 초기 미읽음 카운트 표시 ─────────────────────────────────────────
    fetchUnreadCount();

    // ── 표준 modal-view 등록 패턴 ───────────────────────────────────────
    $modal.data('modal-data', $modal);
    $modal.addClass('modal-view-applied');
    if ($modal.hasClass('modal-body')) {
        const p = $modal.data('param') || {};
        $modal.init_modal(p);
    }
});
