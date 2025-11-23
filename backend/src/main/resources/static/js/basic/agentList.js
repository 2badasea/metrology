$(function () {
	console.log('++ basic/agentList.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		// 이번 memberJoin 모달의 body
		$modal = $bodyCandidate.first();
	} else {
		// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);
	};

	// 업체관리 리스트 가져오기
	$modal.data_source = {
		api: {
			readData: {
				url: '/apiBasic/getAgentList',
				serializer: (grid_param) => {
					// grid_param = $.extend(grid_param, $('form.search_form', $modal).serializeObject());
					// let search_types = $modal
					// 	.find('form.search_form .search_type')
					// 	.find('option')
					// 	.map(function () {
					// 		if ($(this).val() != 'all') return $(this).val();
					// 	})
					// 	.get();
					grid_param.search_types = 'all';
					grid_param.render_version = 'new';
					return $.param(grid_param);
				},
				method: 'GET',
			},
		},
	};

	// 그리드 정의
	$modal.grid = new Grid({
		el: document.querySelector('.agentList'),
		columns: [
			{
				header: '그룹명',
				name: 'genre',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '업체명',
				name: 'name',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '주소',
				name: 'artist',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '사업자번호',
				name: 'release',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '대표',
				name: 'genre',
				className: 'cursor_pointer',
				align: 'center',
			},

			{
				header: '전화번호',
				name: 'genre',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '이메일',
				name: 'genre',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '담당자',
				name: 'genre',
				className: 'cursor_pointer',
				align: 'center',
			},
			{
				header: '담당자 연락처',
				name: 'genre',
				className: 'cursor_pointer',
				align: 'center',
			},
		],
		data: [
			{
				name: 'Beautiful Lies',
				artist: 'Birdy',
				release: '2016.03.26',
				genre: 'Pop',
			},
		],
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
