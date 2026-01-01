$(function () {
	console.log('++ basic/itemCode.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	// const $bodyCandidate = $candidates.filter('.modal-body');
	// if ($bodyCandidate.length) {
	// 	$modal = $bodyCandidate.first();
	// } else {
	// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
	$modal = $candidates.first();
	// }
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		// 교정접수 리스트 가져오기
		$modal.data_source = {
			api: {
				readData: {
					url: '/api/basic/getItemCodeList',
					serializer: (grid_param) => {
						// 접수시작/종료일, 세금계산서, 접수유형, 진행상태, 검색타입, 검색키워드를 넘긴다.
						grid_param.orderStartDate = $('form.searchForm .orderStartDate', $modal).val() ?? ''; // 접수일(시작일)
						grid_param.orderEndDate = $('form.searchForm .orderEndDate', $modal).val() ?? ''; // 접수일(마지막)
						grid_param.isTax = $('form.searchForm .isTax', $modal).val() ?? ''; // 세금계산서 발행여부
						grid_param.caliType = $('form.searchForm .caliType', $modal).val() ?? ''; // 교정유형(고정표준실/현장교정)
						grid_param.statusType = $('form.searchForm .statusType', $modal).val() ?? ''; // 진행상태
						grid_param.searchType = $('form.searchForm .searchType', $modal).val() ?? ''; // 검색타입
						grid_param.keyword = $('form.searchForm', $modal).find('#keyword').val() ?? ''; // 검색키워드
						return $.param(grid_param);
					},
					method: 'GET',
				},
			},
		};

		// 그리드 정의
		$modal.middleGrid = new Grid({
			el: document.querySelector('.middleGrid'),
			columns: [
				{
					header: '접수일',
					name: 'orderDate',
					className: 'cursor_pointer',
					align: 'center',
					width: '80',
					formatter: function (data) {
						return !data.value ? '' : data.value;
					},
				},
			],
			pageOptions: {
				perPage: 12,
			},
			rowHeaders: ['checkbox'],
			// data: $modal.middleDataSource,
			rowHeight: 'auto',
		});

		// 그리드 정의
		$modal.smallGrid = new Grid({
			el: document.querySelector('.smallGrid'),
			columns: [
				{
					header: '접수일',
					name: 'orderDate',
					className: 'cursor_pointer',
					align: 'center',
					width: '80',
					formatter: function (data) {
						return !data.value ? '' : data.value;
					},
				},
			],
			pageOptions: {
				perPage: 8,
			},
			// data: $modal.smallDataSource,
			rowHeaders: ['checkbox'],
			rowHeight: 'auto',
		});

		// 그리드 이벤트 정의
		// $modal.grid.on('click', async function (e) {
		// 	const row = $modal.grid.getRow(e.rowKey);

		// 	if (row && e.columnName != '_checked') {
		// 	}
		// });

	}; // End init_modal

	// 페이지 내 이벤트
	$modal
		// 대분류관리 모달 호출
		.on('click', '.manageBig', async function () {
			console.log('대분류관리 모달 호출');
			const resModal = await g_modal('/basic/bigItemCodeModify', {}, {
				size: 'lg',
				title: '대분류코드 관리',
				show_close_button: true,
				show_confirm_button: true,
			});

			console.log(resModal);
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
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
