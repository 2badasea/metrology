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
			if (resModal && resModal.returnData != undefined) {
				// 업체데이터를 세팅한다.
				const searchAgentInfo = resModal.returnData;

				// 신청업체, 성적서업체 구분
				if (agentFlag == 1) {
					// 업체명, 업체명(영문), fax, 연락처, fx, 교정주기, 주소(국/영문), 담당자(이름, 연락처, 이메일)
					$('input[name=custAgent]', $modal).val(searchAgentInfo.name);
					$('input[name=custAgentIdx]', $modal).val(searchAgentInfo.id);
					$('input[name=custAgentEn]', $modal).val(searchAgentInfo.nameEn);
					$('input[name=custAgentAddr]', $modal).val(searchAgentInfo.addr);
					$('input[name=custAgentAddrEn]', $modal).val(searchAgentInfo.addrEn);
					$('input[name=custAgentTel]', $modal).val(searchAgentInfo.tel);
					$('input[name=custAgentFax]', $modal).val(searchAgentInfo.fax);
					$('input[name=custManager]', $modal).val(searchAgentInfo.managerName);
					$('input[name=custManagerTel]', $modal).val(searchAgentInfo.managerTel);
					$('input[name=custManagerEmail]', $modal).val(searchAgentInfo.managerEmail);
					if (searchAgentInfo.calibrationCycle == 'self_cycle') {
						$('input[name=custAgentCaliCycle]').val('self_cycle');
					} else {
						$('input[name=custAgentCaliCycle]').val('next_cycle');
					}
				}
				// 성적서발행처 조회 시
				else if (agentFlag == 4) {
					// 발행처 (국/영), 주소(국/영), 담당자 (이름, 연락처, 이메일), 소재지주소?
					$('input[name=reportAgentIdx]', $modal).val(searchAgentInfo.id);
					$('input[name=reportAgent]', $modal).val(searchAgentInfo.name);
					$('input[name=reportAgentEn]', $modal).val(searchAgentInfo.nameEn);
					$('input[name=reportAgentAddr]', $modal).val(searchAgentInfo.addr);
					$('input[name=reportAgentAddrEn]', $modal).val(searchAgentInfo.addrEn);
					$('input[name=reportManager]', $modal).val(searchAgentInfo.managerName);
					$('input[name=reportManagerTel]', $modal).val(searchAgentInfo.managerTel);
					$('input[name=reportManagerEmail]', $modal).val(searchAgentInfo.managerEmail);
					// 교정유형이 '현장교정(site)'인 경우, 성적서발행처 주소와 동일하게 소재지 주소 삽입
					if ($('input[name=caliType]:checked', $modal).val() == 'site') {
						$('input[name=siteAddr]', $modal).val(searchAgentInfo.addr);
						$('input[name=siteAddrEn]', $modal).val(searchAgentInfo.addrEn);
					}
				}
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
		})
		// 업체담당자 조회
		.on('click', '.agentManagerSearch', async function () {
			const agentType = $(this).data('type');
			let agentId = 0;
			let agentTypeKr = "";
			if (agentType == 'custManager') {
				agentId = $('input[name=custAgentIdx]', $modal).val();
				agentTypeKr = '신청업체';
			} else {
				agentId = $('input[name=reportAgentIdx]', $modal).val();
				agentTypeKr = '성적서발행처';
			}

			if (agentId == 0) {
				g_toast(`${agentTypeKr}가 조회되지 않았습니다.<br>업체부터 선택해주세요.`, 'warning');
				return false;
			} else {
				const resModal = await g_modal('/agent/searchAgentManager', {
					agentId: agentId
				}, {
					// '닫기'버튼만 표시
					title: `${agentTypeKr} 조회`,
					size: 'lg',
					show_close_button: true,
					show_confirm_button: false,
					confirm_button_text: '저장',
				});

				if (resModal && resModal.managerInfo != undefined) {
					const managerInfo = resModal.managerInfo;
					console.log("🚀 ~ managerInfo:", managerInfo);
					
				}




			}


		})
		;

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
