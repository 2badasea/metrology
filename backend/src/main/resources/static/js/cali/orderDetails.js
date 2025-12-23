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
		console.log('🚀 ~ caliOrderId:', caliOrderId);
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
				header: '구분',
				name: 'reportType',
				className: 'cursor_pointer',
				width: '50',
				align: 'center',
				formatter: function (data) {
					return data.value == 'SELF' ? '자체' : '대행';
				},
			},
			{
				header: '접수타입',
				name: 'orderType',
				className: 'cursor_pointer',
				width: '70',
				align: 'center',
				formatter: function (data) {
					return data.value == 'ACCREDDIT' ? '공인' : data.value == 'UNACCREDDIT' ? '비공인' : '시험';
				},
			},
			{
				header: '중분류코드',
				name: 'middleItemCodeNum',
				className: 'cursor_pointer',
				width: '70',
				align: 'center',
			},
			{
				header: '소분류코드',
				name: 'smallItemCodeNum',
				className: 'cursor_pointer',
				width: '70',
				align: 'center',
			},
			{
				header: '성적서번호',
				name: 'reportNum',
				className: 'cursor_pointer',
				width: '100',
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
				width: '120',
				align: 'center',
			},
			{
				header: '형식',
				name: 'itemFormat',
				className: 'cursor_pointer',
				width: '120',
				align: 'center',
			},
			{
				header: '기기번호',
				name: 'itemNum',
				className: 'cursor_pointer',
				width: '120',
				align: 'center',
			},
			{
				header: '관리번호',
				name: 'manageNo',
				className: 'cursor_pointer',
				width: '70',
				align: 'center',
			},
			{
				header: '진행상태',
				name: 'statusTxt',
				className: 'cursor_pointer',
				width: '50',
				align: 'center',
				formatter: function (data) {
					// TODO 별도로 상태값에 맞는 formatter 생성해서 이용할 것
					return '';
				}
			},
		],
		pageOptions: {
			useClient: false, // 서버 페이징
			perPage: 20,		// 기본 20. 선택한 '행 수'에 따라 유동적으로 변경
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
