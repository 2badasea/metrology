$(function () {
	console.log('++ member/memberManage.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	// 직원관리 리스트
	$modal.init_modal = (param) => {
		$modal.param = param;

		// 업체관리 리스트 가져오기
		$modal.data_source = {
			api: {
				readData: {
					url: '/api/member/getMemberList',
					// 'serializer'는 토스트 그리드에서 제공
					serializer: (grid_param) => {
						grid_param.workType = $modal.find('select[name=workType]').val(); // 재직여부
						grid_param.searchType = $modal.find('select[name=searchType]').val(); // 검색 타입
						grid_param.keyword = $modal.find('input[name=keyword]').val(); // 검색 키워드
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 직원관리 그리드
		$modal.grid = gGrid('.memberList', {
			columns: [
				// 사번, 아이디, 이메일, 이름, 영문이름, 휴대번호, 부서, 직급, 상태(재직여부)
				{
					header: '사번',
					name: 'companyNo',
					className: 'cursor_pointer',
					width: '200',
					align: 'center',
				},
				{
					header: '아이디',
					name: 'loginId',
					className: 'cursor_pointer',
					align: 'center',
					width: '200',
					sortable: true,
				},
				{
					header: '이메일',
					name: 'email',
					className: 'cursor_pointer',
					// width: '200',
					align: 'center',
					sortable: true,
				},
				{
					header: '이름',
					name: 'name',
					width: '130',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '휴대번호',
					name: 'hp',
					className: 'cursor_pointer',
					width: '130',
					align: 'center',
				},
				{
					header: '부서',
					name: 'departmentName',
					width: '130',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '직급',
					name: 'levelName',
					className: 'cursor_pointer',
					width: '130',
					align: 'center',
				},
				{
					header: '상태(재직유무)',
					name: 'workType',
					className: 'cursor_pointer',
					width: '130',
					align: 'center',
					formatter: function ({ row, value }) {
						return value == 0 ? '재직' : value == 1 ? '휴직' : '퇴직';
					},
				},
				{
					header: '메뉴권한',
					name: 'menuPermission',
					width: '90',
					align: 'center',
					formatter: function () {
						return '<i class="bi bi-shield-lock" style="font-size:1.1rem;cursor:pointer;"></i>';
					},
				},
			],
			pageOptions: {
				useClient: false, // 서버 페이징
				perPage: 20,
			},
			rowHeaders: ['checkbox'],
			rowHeight: 'auto',
			minBodyHeight: 663,
			bodyHeight: 663,
			data: $modal.data_source,
		});

		// 서버 데이터 수신 후 레이아웃 재계산 (헤더-바디 열 정렬 보정)
		$modal.grid.on('response', () => $modal.grid.refreshLayout());

		// 그리드 이벤트 정의
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);
			if (!row || e.columnName === '_checked') return;

			// 메뉴권한 열 클릭
			if (e.columnName === 'menuPermission') {
				if (G_USER.auth !== 'admin') {
					gToast('권한이 없습니다.', 'warning');
					return;
				}
				await gModal(
					'/member/menuPermission',
					{ memberId: row.id, memberName: row.name },
					{
						title: row.name + ' 메뉴권한',
						size: 'md',
						show_close_button: true,
						show_confirm_button: true,
						confirm_button_text: '저장',
					},
				);
				return;
			}

			// 그 외 열 클릭 → 직원 등록/수정 페이지 이동
			location.href = `/member/memberModify?id=${row.id}`;
		});

	};	// End init_modal

	// 페이지 내 이벤트
	$modal
		// 검색
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.grid.getPagination().movePageTo(1);
		})
		// 삭제
		.on('click', '.deleteMember', async function (e) {
			e.preventDefault();
			const checkedRows = $modal.grid.getCheckedRows();
			if (!checkedRows || checkedRows.length === 0) {
				gToast('삭제할 직원을 선택해주세요.', 'warning');
				return false;
			}
			const ids = checkedRows.map((row) => row.id);
			const deleteConfirm = await gMessage('직원 삭제', `선택한 직원 ${ids.length}명을 삭제하시겠습니까?`, 'question', 'confirm');
			if (!deleteConfirm.isConfirmed) return false;

			gLoadingMessage();
			try {
				const res = await fetch('/api/member/members', {
					method: 'DELETE',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ ids }),
				});
				Swal.close();
				if (res.ok) {
					const resData = await res.json();
					if (resData?.code > 0) {
						await gMessage('직원 삭제', '삭제되었습니다.', 'success', 'alert');
						$modal.grid.getPagination().movePageTo(1);
					} else {
						await gMessage('직원 삭제', resData?.msg ?? '삭제에 실패했습니다.', 'warning', 'alert');
					}
				} else {
					const errData = await res.json().catch(() => null);
					await gMessage('직원 삭제', errData?.msg ?? '서버 오류가 발생했습니다.', 'warning', 'alert');
				}
			} catch (xhr) {
				gApiErrorHandler(xhr);
			} finally {
				Swal.close();
			}
		})
		// 등록
		.on('click', '.addMember', async function (e) {
			e.preventDefault();
			location.href = `/member/memberModify`;
		});

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		//모달 팝업창인경우
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
		// 모달이 아닌 일반 페이지인 경우엔 아래 initPage가 동작한다.
		if (!$modal_root.length) {
			initPage($modal);
		}
	}
});
