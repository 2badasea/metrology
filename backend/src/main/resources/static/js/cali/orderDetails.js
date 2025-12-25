$(function () {
	console.log('++ cali/orderDetails.js');

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

	let caliOrderId = null;
	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		caliOrderId = document.getElementById('caliOrderId').value;

		// 성적서 리스트 가져오기
		$modal.data_source = {
			api: {
				readData: {
					url: '/api/report/getOrderDetailsList',
					serializer: (grid_param) => {
						// TODO item, item_code 테이블 생성 이후에 중분류/소분류 필터링도 검색조건 추가 필요
						grid_param.orderType = $('form.searchForm .orderType', $modal).val() ?? ''; // 전체선택은 all로 간주
						grid_param.statusType = $('form.searchForm .statusType', $modal).val() ?? ''; // 진행상태
						grid_param.searchType = $('form.searchForm .searchType', $modal).val() ?? 'all'; // 검색타입
						grid_param.keyword = $('form.searchForm', $modal).find('#keyword').val() ?? ''; // 검색키워드
						grid_param.caliOrderId = caliOrderId; // 접수 id

						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.grid = new Grid({
			el: document.querySelector('.reportList'),
			columns: [
				{
					header: '구분',
					name: 'reportType',
					className: 'cursor_pointer',
					width: '60',
					align: 'center',
					formatter: function (data) {
						return data.value == 'SELF' ? '자체' : '대행';
					},
				},
				{
					header: '접수타입',
					name: 'orderType',
					className: 'cursor_pointer',
					width: '80',
					align: 'center',
					formatter: function (data) {
						return data.value == 'ACCREDDIT' ? '공인' : data.value == 'UNACCREDDIT' ? '비공인' : '시험';
					},
				},
				{
					header: '중분류코드',
					name: 'middleItemCodeNum',
					className: 'cursor_pointer',
					width: '100',
					align: 'center',
				},
				{
					header: '소분류코드',
					name: 'smallItemCodeNum',
					className: 'cursor_pointer',
					width: '100',
					align: 'center',
				},
				{
					header: '성적서번호',
					name: 'reportNum',
					className: 'cursor_pointer',
					width: '120',
					align: 'center',
				},
				{
					header: '기기명',
					name: 'itemName',
					className: 'cursor_pointer',
					// width: '120',
					align: 'center',
				},
				{
					header: '제작회사',
					name: 'itemMakeAgent',
					className: 'cursor_pointer',
					width: '200',
					align: 'center',
				},
				{
					header: '형식',
					name: 'itemFormat',
					className: 'cursor_pointer',
					width: '200',
					align: 'center',
				},
				{
					header: '기기번호',
					name: 'itemNum',
					className: 'cursor_pointer',
					width: '150',
					align: 'center',
				},
				{
					header: '관리번호',
					name: 'manageNo',
					className: 'cursor_pointer',
					width: '120',
					align: 'center',
				},
				{
					// 값이 아닌 formatter로 보여줄 것
					header: '진행상태',
					name: 'reportStatus',
					className: 'cursor_pointer',
					width: '70',
					align: 'center',
					formatter: function (data) {
						// TODO 별도로 상태값에 맞는 formatter 생성해서 이용할 것
						return '';
					},
				},
			],
			pageOptions: {
				useClient: false, // 서버 페이징 
				perPage: 20, // 기본 20. 선택한 '행 수'에 따라 유동적으로 변경	=> change 이벤트를 통해 setPerPage() 함수 호출
			},
			rowHeaders: ['checkbox'],
			minBodyHeight: 663,
			bodyHeight: 663,
			data: $modal.data_source,
			rowHeight: 'auto',
		});

		// 그리드 이벤트 정의
		$modal.grid.on('click', async function (e) {
			const row = $modal.grid.getRow(e.rowKey);

			if (row && e.columnName != '_checked') {
			}
		});
	};

	// 페이지 내 이벤트 정의
	$modal
		// 성적서 등록 모달 호출
		.on('submit', '.searchForm', function (e) {
			e.preventDefault();
			$modal.grid.getPagination().movePageTo(1); // 변경된 페이지 옵션에 맞춰 페이지 렌더링
		})
		.on('click', '.addReport', async function () {
			const resModal = await g_modal(
				'/cali/registerMultiReport',
				{
					caliOrderId: caliOrderId,
				},
				{
					title: '성적서 등록',
					size: 'xxxl',
					show_close_button: true,
					show_confirm_button: true,
					confirm_button_text: '저장',
					custom_btn_html_arr: [
						`<button type="button" class="btn btn-success addReportExcel btn-sm"><i class="bi bi-file-excel"></i>EXCEL 등록</button>`,
					],
				}
			);
		})
		// 행 수 변경
		.on('change', '.rowLeng', function () {
			const rowLeng = $(this).val();
			console.log('🚀 ~ rowLeng:', rowLeng);

			if (rowLeng > 0) {
				$modal.grid.setPerPage(rowLeng); // perPage옵션 동적 변경
				// $modal.grid.reaPage(1);	// setPerPage() 호출 후, 굳이 readPage() 호출할 필요없음.
				// setPerPage()와 아래 getPagination().movePageTo()는 잘 사용되지 않는 옵션이라 함(내용확인!)
				// $modal.grid.getPagination().movePageTo(1);	// 변경된 페이지 옵션에 맞춰 페이지 렌더링
			}
		})
		// 성적서 삭제
		.on('click', '.deleteReport', function () {
			const checkedRows = $modal.grid.getCheckedRows();
			if (checkedRows.length === 0) {
				g_toast('삭제할 성적서를 선택해주세요.', 'warning');
				return false;
			}

			// 선택된 성적서들이 해당 페이지에서 접수구분별 가장 마지막에 속하는지, 결재가 진행중인 건이 있는지 확인

		})
		;

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
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
