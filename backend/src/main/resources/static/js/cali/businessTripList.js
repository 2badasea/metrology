console.log('++ cali/businessTripList.js');

$(function () {

    /* ================================================================
       출장일정 리스트 페이지
       - TUI Grid 서버사이드 페이지네이션 (workApproval.js 동일 패턴)
       - 검색 필터: searchType + keyword + dateStart/dateEnd
       - 등록/수정/삭제 기능 (수정·삭제는 수정 모달 경유)
    ================================================================ */

    const $candidates = $('.modal-view:not(.modal-view-applied)');
    let $modal;
    $modal = $candidates.first();
    let $modal_root = $modal.closest('.modal');

    // ──────────────────────────────────────────
    // 날짜 formatter: LocalDateTime → 'YYYY-MM-DD HH:mm'
    // ──────────────────────────────────────────
    function formatDatetime(value) {
        if (!value) return '';
        const d = new Date(value);
        if (isNaN(d.getTime())) return String(value);
        const pad = (n) => String(n).padStart(2, '0');
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }

    // ──────────────────────────────────────────
    // 파일 버튼 formatter
    // - fileCnt > 0 → 파란 버튼(건수 표시)
    // - fileCnt === 0 → 회색 버튼(아이콘만)
    // ──────────────────────────────────────────
    function fileCntFormatter({ value }) {
        const cnt = value != null ? Number(value) : 0;
        const cls = cnt > 0 ? 'btn-primary' : 'btn-secondary';
        const label = cnt > 0
            ? `<i class="bi bi-folder2-open"></i> ${cnt}`
            : `<i class="bi bi-folder2-open"></i>`;
        return `<button type="button" class="btn ${cls} btn-sm"
                    style="width:100%;height:100%;min-height:26px;border-radius:0;border:0;padding:0 4px;">
                    ${label}
                </button>`;
    }

    // ──────────────────────────────────────────
    // 접수 버튼 formatter (미구현)
    // ──────────────────────────────────────────
    function receiptFormatter() {
        return `<button type="button" class="btn btn-outline-secondary btn-sm"
                    style="width:100%;height:100%;min-height:26px;border-radius:0;border:0;padding:0;">
                    접수
                </button>`;
    }

    // ──────────────────────────────────────────
    // init_modal — 페이지/모달 공통 초기화 훅
    // ──────────────────────────────────────────
    $modal.init_modal = async (param) => {
        $modal.param = param;

        // breadcrumb 세팅 (menu 테이블 미등록 페이지이므로 직접 표시)
        $('.topbar-inner .customBreadcrumb').text('교정관리 - 출장일정 리스트 보기');

        // 날짜 필터 기본값: 이달 1일 ~ 이달 말일
        const now        = new Date();
        const yyyy       = now.getFullYear();
        const mm         = String(now.getMonth() + 1).padStart(2, '0');
        const lastDay    = new Date(yyyy, now.getMonth() + 1, 0).getDate();
        const dateStart  = `${yyyy}-${mm}-01`;
        const dateEnd    = `${yyyy}-${mm}-${String(lastDay).padStart(2, '0')}`;
        $('#dateStart', $modal).val(dateStart);
        $('#dateEnd',   $modal).val(dateEnd);

        // 초기 데이터 조회 (1페이지)
        $modal.grid.getPagination().movePageTo(1);
    };

    // ──────────────────────────────────────────
    // 그리드 데이터 소스 (TUI Grid 서버사이드 페이지네이션)
    // serializer: 그리드가 페이지 이동/정렬 시 호출 → 검색 파라미터 추가
    // ──────────────────────────────────────────
    $modal.dataSource = {
        api: {
            readData: {
                url: '/api/admin/businessTrip/list',
                method: 'GET',
                serializer: (gridParam) => {
                    gridParam.searchType = $('#searchType', $modal).val() ?? '';
                    gridParam.keyword    = $('#keyword',    $modal).val()?.trim() ?? '';
                    gridParam.dateStart  = $('#dateStart',  $modal).val() ?? '';
                    gridParam.dateEnd    = $('#dateEnd',    $modal).val() ?? '';
                    return $.param(gridParam);
                },
            },
        },
    };

    // ──────────────────────────────────────────
    // 그리드 초기화
    // rowHeaders: ['checkbox'] → 첫 번째 열 체크박스 (일괄선택/해제)
    // scrollX: true → 열이 많으므로 가로 스크롤 허용
    // pageOptions.useClient: false → 서버사이드 페이지네이션
    // ──────────────────────────────────────────
    $modal.grid = gGrid('#tripListGrid', {
        rowHeaders: ['checkbox'],
        columns: [
            {
                header: '유형',
                name: 'type',
                width: 70,
                align: 'center',
            },
            {
                header: '출장시작일시',
                name: 'startDatetime',
                width: 135,
                align: 'center',
                formatter: ({ value }) => formatDatetime(value),
            },
            {
                header: '출장종료일시',
                name: 'endDatetime',
                width: 135,
                align: 'center',
                formatter: ({ value }) => formatDatetime(value),
            },
            {
                header: '일정제목',
                name: 'title',
                minWidth: 130,
            },
            {
                header: '신청업체',
                name: 'custAgent',
                width: 120,
            },
            {
                header: '신청업체주소',
                name: 'custAgentAddr',
                width: 150,
            },
            {
                header: '성적서발행처',
                name: 'reportAgent',
                width: 120,
            },
            {
                header: '성적서발행처주소',
                name: 'reportAgentAddr',
                width: 160,
            },
            {
                header: '담당자',
                name: 'custManager',
                width: 80,
            },
            {
                header: '담당자 연락처',
                name: 'custManagerTel',
                width: 115,
            },
            {
                header: '파일',
                name: 'fileCnt',
                width: 65,
                align: 'center',
                sortable: false,
                formatter: fileCntFormatter,
            },
            {
                header: '출장자',
                name: 'travelerNames',
                width: 130,
            },
            {
                header: '접수',
                name: 'receiptBtn',
                width: 60,
                align: 'center',
                sortable: false,
                formatter: receiptFormatter,
            },
        ],
        scrollX: true,
        scrollY: false,
        minBodyHeight: 600,
        bodyHeight: 600,
        pageOptions: {
            useClient: false,   // 서버 페이징
            perPage: 25,
        },
        data: $modal.dataSource,
    });

    // ──────────────────────────────────────────
    // 그리드 셀 클릭 이벤트
    // - _checked(체크박스) → 무시
    // - fileCnt 셀 클릭 → 첨부파일 모달 오픈
    // - receiptBtn 셀 클릭 → 구현 준비중 gToast
    // - 그 외 셀 클릭 → 수정 모달 오픈
    // ──────────────────────────────────────────
    $modal.grid.on('click', async function (ev) {
        if (ev.columnName === '_checked') return;
        if (ev.rowKey === undefined || ev.rowKey === null) return;

        const rowData = $modal.grid.getRow(ev.rowKey);
        if (!rowData || !rowData.id) return;

        if (ev.columnName === 'fileCnt') {
            const fileCnt = rowData.fileCnt != null ? Number(rowData.fileCnt) : 0;
            if (fileCnt <= 0) {
                gToast('등록된 첨부파일이 없습니다.', 'warning');
                return;
            }
            const fileResult = await gModal(
                '/basic/fileList',
                { refTableName: 'business_trip', refTableId: rowData.id },
                { size: 'lg', title: '첨부파일 확인', show_close_button: true, show_confirm_button: false }
            );
            // 파일 전체 삭제 후 닫힌 경우 → 현재 페이지 재로드
            if (fileResult?.fileCnt === 0) {
                const currentPage = $modal.grid.getPagination()?.getCurrentPage() ?? 1;
                $modal.grid.getPagination().movePageTo(currentPage);
            }
        } else if (ev.columnName === 'receiptBtn') {
            gToast('구현 준비중입니다.', 'info');
        } else {
            await openModifyModal(rowData.id);
        }
    });

    // ──────────────────────────────────────────
    // 수정 모달 오픈
    // - custom_btn_html_arr로 삭제 버튼 추가 (businessTrip.js와 동일)
    // - 닫힘 후 현재 페이지 유지하며 재조회
    // ──────────────────────────────────────────
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
                // 클릭 핸들러는 businessTripModify.js에서 처리
                custom_btn_html_arr: [
                    '<button type="button" class="btn btn-danger btn-sm modal-btn-delete-btrip mr-auto">삭제</button>',
                ],
            }
        );
        const currentPage = $modal.grid.getPagination()?.getCurrentPage() ?? 1;
        $modal.grid.getPagination().movePageTo(currentPage);
    }

    // ──────────────────────────────────────────
    // 등록 모달 오픈
    // - 오늘 날짜를 defaultDate로 전달 → 모달에서 08:00/17:00 기본값 세팅
    // - 닫힘 후 1페이지로 이동 (새 항목이 최신순 상단에 위치)
    // ──────────────────────────────────────────
    async function openRegisterModal() {
        const today = new Date().toISOString().substring(0, 10);
        await gModal(
            '/cali/businessTripModify',
            { id: null, defaultDate: today },
            {
                title: '출장일정 등록',
                size: 'xxl',
                show_close_button: true,
                show_confirm_button: true,
                confirm_button_text: '저장',
            }
        );
        $modal.grid.getPagination().movePageTo(1);
    }

    // ──────────────────────────────────────────
    // 이벤트 바인딩
    // ──────────────────────────────────────────
    $modal
        // 검색 버튼 — 날짜 역순 입력 시 gToast + 날짜 초기화
        .on('click', '#btnSearch', function () {
            const dateStart = $('#dateStart', $modal).val();
            const dateEnd   = $('#dateEnd',   $modal).val();
            if (dateStart && dateEnd && new Date(dateStart) > new Date(dateEnd)) {
                gToast('시작일이 종료일보다 늦습니다. 날짜를 다시 선택해주세요.', 'warning');
                $('#dateStart', $modal).val('');
                $('#dateEnd',   $modal).val('');
                return;
            }
            $modal.grid.getPagination().movePageTo(1);
        })
        // 초기화 버튼 — 날짜 기본값도 이달 범위로 복원
        .on('click', '#btnReset', function () {
            const now     = new Date();
            const yyyy    = now.getFullYear();
            const mm      = String(now.getMonth() + 1).padStart(2, '0');
            const lastDay = new Date(yyyy, now.getMonth() + 1, 0).getDate();
            $('#searchType', $modal).val('title');
            $('#keyword',    $modal).val('');
            $('#dateStart',  $modal).val(`${yyyy}-${mm}-01`);
            $('#dateEnd',    $modal).val(`${yyyy}-${mm}-${String(lastDay).padStart(2, '0')}`);
            $modal.grid.getPagination().movePageTo(1);
        })
        // 키워드 입력창 Enter → 검색
        .on('keydown', '#keyword', function (e) {
            if (e.key === 'Enter') $('#btnSearch', $modal).trigger('click');
        })
        // 등록 버튼
        .on('click', '#btnRegister', function () {
            openRegisterModal();
        })
        // 삭제 버튼 — 체크박스로 선택된 행 일괄 삭제
        // DELETE /api/admin/businessTrip → Security 필터에서 ROLE_ADMIN 자동 적용
        .on('click', '#btnDelete', async function () {
            const checkedRows = $modal.grid.getCheckedRows();
            if (!checkedRows || checkedRows.length === 0) {
                gToast('삭제할 항목을 선택해주세요.', 'warning');
                return;
            }

            const confirmResult = await gMessage(
                `출장일정 ${checkedRows.length}건을 삭제하시겠습니까?`,
                '삭제된 출장일정은 복구할 수 없습니다.',
                'warning',
                'confirm'
            );
            if (confirmResult.isConfirmed !== true) return;

            gLoadingMessage('삭제 중입니다...');
            try {
                const ids = checkedRows.map(r => r.id);
                const res = await fetch('/api/admin/businessTrip', {
                    method:  'DELETE',
                    headers: { 'Content-Type': 'application/json' },
                    body:    JSON.stringify({ ids }),
                });
                if (!res.ok) throw res;

                Swal.close();
                gToast(`${checkedRows.length}건이 삭제되었습니다.`, 'success');
                const currentPage = $modal.grid.getPagination()?.getCurrentPage() ?? 1;
                $modal.grid.getPagination().movePageTo(currentPage);
            } catch (e) {
                Swal.close();
                console.error('출장일정 삭제 실패', e);
                gApiErrorHandler(e);
            }
        })
        // 캘린더보기 버튼
        .on('click', '#btnCalendar', function () {
            window.location.href = '/cali/businessTrip';
        })
        // 행수 select — perPage 변경 후 1페이지로 이동
        .on('change', '#perPageSelect', function () {
            const perPage = parseInt($(this).val(), 10) || 25;
            $modal.grid.getPagination().setPerPage(perPage);
            $modal.grid.getPagination().movePageTo(1);
        });

    // ── 표준 모달 초기화 footer (common.js 규약) ────────────────────
    $modal.data('modal-data', $modal);
    $modal.addClass('modal-view-applied');
    if ($modal.hasClass('modal-body')) {
        // 모달 팝업창인 경우
        $modal_root.on('modal_ready', function (e, p) {
            $modal.init_modal(p);
            if (typeof $modal.grid == 'object') {
                $modal.grid.refreshLayout();
            }
        });
    }

    if (typeof window.modal_deferred == 'object') {
        window.modal_deferred.resolve('script end');
    } else {
        if (!$modal_root.length) {
            initPage($modal);
        }
    }

});
