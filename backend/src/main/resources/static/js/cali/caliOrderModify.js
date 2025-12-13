$(function () {
	console.log('++ cali/caliOrderModify.js');

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

	let caliOrderId = null; // 업체id

	$modal.init_modal = async (param) => {
		$modal.param = param;

		let gridBodyHeight = Math.floor($modal.find('.caliOrderModifyForm').height() - 88);

		// 업체id로 초기화 하기(수정)
		if ($modal.param?.caliOrderId > 0) {
			// 옵셔널체이닝으로 체크
			caliOrderId = Number($modal.param.caliOrderId);

			// NOTE async, await으로도 가능한지 확인
			try {
			} catch (err) {
				custom_ajax_handler(err);
			} finally {
			}
		}

		// 수정인 경우, 담당자 리스트 정보 세팅
		// $modal.dataSource = {
		// 	api: {
		// 		readData: {
		// 			url: '/api/basic/getAgentManagerList',
		// 			serializer: (grid_param) => {
		// 				grid_param.agentId = agentId;
		// 				grid_param.isVisible = 'y';
		// 				return $.param(grid_param);
		// 			},
		// 			method: 'GET',
		// 		},
		// 	},
		// };

		// 업체 담당자 그리드
		// $modal.grid = new Grid({
		// 	el: document.querySelector('.reportList'),
		// 	columns: [
		// 		{
		// 			header: '담당자명',
		// 			name: 'name',
		// 			className: 'cursor_pointer',
		// 			editor: 'text',
		// 			width: '150',
		// 			align: 'center',
		// 		},
		// 		{
		// 			header: '담당자 이메일',
		// 			name: 'email',
		// 			editor: 'text',
		// 			className: 'cursor_pointer',
		// 			align: 'center',
		// 		},
		// 	],
		// 	minBodyHeight: gridBodyHeight,
		// 	bodyHeight: gridBodyHeight,
		// 	editingEvent: 'click', // 원클릭으로 수정할 수 있도록 변경. 기본값은 'dblclick'
		// 	// data: $modal.dataSource,
		// 	pageOptions: {
		// 		perPage: 15
		// 	},
		// });

		// 업체조회 함수 정의
		$modal.searchAgent = async (type, agentName) => {
			const agentFlag = type == 'custAgent' ? 1 : 4;
			console.log('🚀 ~ agentFlag:', agentFlag);

			// g_modal 호출
			const resModal = await g_modal(
				'/agent/searchAgentModify',
				{
					agentFlag: agentFlag,
					agentName: agentName,
				},
				{
					title: '업체 조회',
					size: 'xxl',
					show_close_button: true,
					show_confirm_button: false,
					confirm_button_text: '저장',
					custom_btn_html_arr: [
						`<button type="button" class="btn btn-primary addAgent btn-sm"><i class="bi bi-plus-square"></i>업체등록</button>`,
					],
				}
			);

			// 리턴값 확인
			console.log('🚀 ~ resModal:', resModal);
			if (resModal) {
				// 업체 데이터 갱신하기
			}
		};
	};

	// 모달 내 이벤트 정의
	$modal
		// 업체조회 클릭 시 모달 호출
		.on('click', '.searchAgent', function () {
			const $btn = $(this);
			const type = $btn.data('type');
			const agnetName = $(`input[name=${type}`, $modal).val();
			$modal.searchAgent(type, agnetName);
		})
		// 업체명 항목에 enter클릭 시, 업체조회 모달 호출
		.on('keyup', '.searchAgentInput', function (e) {
			if (e.key === 'Enter' || e.keyCode === 13) {
				const type = $(this).data('type');
				const agentName = $(this).val();
				$modal.searchAgent(type, agentName);
			}
		});
	// 업체조회 모달 호출
	// .on('click', '.searchAddr')
	// .on('keyup', 'input[name=agentNum]', function (e) {
	// 	// 엔터키 -> 중복체크
	// 	if (e.key === 'Enter' || e.keyCode === 13) {
	// 		$('button.chkAgentNum', $modal).trigger('click'); // 중복확인 요청
	// 		return false;
	// 	} else {
	// 		$modal.agentNumKeyupHandler.call(this, e);
	// 	}
	// })

	// 저장
	$modal.confirm_modal = async function (e) {
		console.log('저장클릭!!');
	};

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
