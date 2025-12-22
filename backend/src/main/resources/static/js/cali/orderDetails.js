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
        console.log("🚀 ~ caliOrderId:", caliOrderId);

	};

	// 성적서 리스트 가져오기
	// $modal.data_source = {
	// 	api: {
	// 		readData: {
	// 			url: '/api/report/getOrderReportList',
	// 			serializer: (grid_param) => {
	// 				return $.param(grid_param);
	// 			},
	// 			method: 'GET',
	// 		},
	// 	},
	// };

	// 그리드 정의
	$modal.grid = new Grid({
		el: document.querySelector('.reportList'),
		columns: [
			{
				header: '긴급여부',
				name: 'priority_type',
				className: 'cursor_pointer',
				width: '60',
				align: 'center',
				formatter: function (data) {
					return data.value == 'emergency' ? '긴급' : '일반';
				},
			},
			{
				// DB상에서는 datetime이지만, 화면에는 date타입으로 표현
				header: '접수일',
				name: 'orderDate',
				className: 'cursor_pointer',
				align: 'center',
				width: '80',
				formatter: function (data) {
					return !data.value ? '' : data.value;
				},
			},
			{
				header: '접수번호',
				name: 'orderNum',
				className: 'cursor_pointer',
				width: '120',
				align: 'center',
			}
		],
		pageOptions: {
			useClient: false, // 서버 페이징
			perPage: 20,
		},
		rowHeaders: ['checkbox'],
		minBodyHeight: 663,
		bodyHeight: 663,
		// data: $modal.data_source,
		rowHeight: 'auto',
	});

	// 그리드 이벤트 정의
	$modal.grid.on('click', async function (e) {
		const row = $modal.grid.getRow(e.rowKey);

		if (row && e.columnName != '_checked') {

		}
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
