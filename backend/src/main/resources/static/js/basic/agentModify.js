$(function () {
	console.log('++ basic/agentModify.js');

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

	let agentId = 0;

	$modal.init_modal = (param) => {
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);

		// 업체id로 초기화 하기(수정)
		if ($modal.param?.id > 0) {
			console.log('업체[수정] 모달 open');
			// 옵셔널체이닝으로 체크
			agentId = Number($modal.param.id);

			// g_ajax로 값 세팅
            // NOTE async, await으로도 가능한지 확인
			g_ajax(
				'/apiBasic/getAgentInfo',
				{
					id: agentId,
				},
				{
					success: function (data) {
						if (data) {
							$modal.find('form.agentModifyForm input[name], textarea[name]').setupValues(data);
							// flag, type에 대해서도 세팅할 것
							// 폐업구분
							if (data.isClose == 'y') {
								$('.isClose', $modal).prop('checked', true);
							}
						}
					},
					error: function (xhr) {
						custom_ajax_handler(xhr);
					},
					complete: function () {
						console.log('업체정보 데이터 세팅 complete');
					},
				}
			);
		} else {
			console.log('업체[등록] 모달 open!');
		}

		// 담당자 리스트
	};

	// 담당자 그리드 초기화

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		//모달 팝업창인 경우 바로 init_modal() 호출
		const p = $modal.data('param') || {};
		$modal.init_modal(p);
		if (typeof $modal.grid == 'object') {
			$modal.grid.refreshLayout();
		}
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
