$(function () {
	console.log('++ agent/searchAgentModify.js');

	// 1) 아직 modal-view-applied 안 된 애들 중에서
	const $notModalViewAppliedEle = $('.modal-view:not(.modal-view-applied)');
	// 2) 모달 안에서 뜨는 경우: .modal-body.modal-view 우선 선택
	const $hasModalBodyEle = $notModalViewAppliedEle.filter('.modal-body');
	if ($hasModalBodyEle.length) {
		$modal = $hasModalBodyEle.first();
	} else {
		// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
		$modal = $notModalViewAppliedEle.first();
	}
	// let $modal = $('.modal-view:not(.modal-view-applied)');
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = async (param) => {
		$modal.param = param;

		// $modal.data_source = {
		// 	api: {
		// 		readData: {
		// 			url: '/api/basic/getAgentList',
		// 			// 'serializer'는 토스트 그리드에서 제공
		// 			serializer: (grid_param) => {
		// 				grid_param.inputAgentName = $('.inputAgentName', $modal).val() ?? '';
		// 				grid_param.inputAgentAddr = $('.inputAgentAddr', $modal).val() ?? '';
		// 				grid_param.inputAgentNum = $('.inputAgentNum', $modal).val() ?? '';
		// 				return $.param(grid_param);
		// 			},
		// 			method: 'GET',
		// 		},
		// 	},
		};

		console.log('ss');

		// 그리드 정의
		$modal.grid = new Grid({
			el: document.querySelector('.searchAgentList'),
			columns: [
				{
					header: '가입방식',
					name: 'createType',
					className: 'cursor_pointer',
					width: '80',
					align: 'center',
					formatter: function (data) {
						let html = '';
						if (data.value == 'join') {
							html = '가입';
						} else if (data.value == 'basic') {
							html = '등록';
						} else if (data.value == 'auto') {
							html = '접수';
						}
						return html;
					},
				},
				{
					header: '그룹명',
					name: 'groupName',
					className: 'cursor_pointer',
					width: '100',
					align: 'center',
				},
				{
					header: '업체명',
					name: 'name',
					className: 'cursor_pointer',
					align: 'center',
					sortable: true,
				},
				{
					header: '주소',
					name: 'addr',
					className: 'cursor_pointer',
					width: '300',
					align: 'center',
					sortable: true,
				},
				{
					header: '사업자번호',
					name: 'agentNum',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '대표',
					name: 'ceo',
					className: 'cursor_pointer',
					width: '80',
					align: 'center',
				},

				{
					header: '전화번호',
					name: 'agnetTel',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '이메일',
					name: 'email',
					className: 'cursor_pointer',
					align: 'center',
				},
				{
					header: '담당자',
					name: 'manager',
					className: 'cursor_pointer',
					width: '80',
					align: 'center',
				},
				{
					header: '담당자 연락처',
					name: 'managerTel',
					className: 'cursor_pointer',
					align: 'center',
				},
			],
			pageOptions: {
				useClient: false, // 서버 페이징
				perPage: 15,
			},
			minBodyHeight: 663,
			bodyHeight: 663,
			// data: $modal.data_source
		});
	};

	// 모달 내 이벤트 정의
	// $modal;

	// 저장
	$modal.confirm_modal = async function (e) {};

	// 담당자 그리드 초기화

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		//모달 팝업창인 경우 바로 init_modal() 호출
		setTimeout(() => {
			const p = $modal.data('param') || {};
			$modal.init_modal(p);
			if (typeof $modal.grid == 'object') {
				$modal.grid.refreshLayout();
			}
		}, 200);
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
