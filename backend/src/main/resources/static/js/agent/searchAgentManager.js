$(function () {
	console.log('++ agent/searchAgentManager.js');

	const $notModalViewAppliedEle = $('.modal-view:not(.modal-view-applied)');
	const $hasModalBodyEle = $notModalViewAppliedEle.filter('.modal-body');
	if ($hasModalBodyEle.length) {
		$modal = $hasModalBodyEle.first();
	} else {
		$modal = $notModalViewAppliedEle.first();
	}
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = (param) => {
		$modal.param = param;
	};

	// 모달 내 이벤트 정의
	$modal
		// 행 클릭 시, 해당 담당자의 정보가 세팅된다.
		.on('click', '.agentManagerTb tr', function () {
			const $tr = $(this);
			const managerInfo = {
				name: $tr.find('td.name').text(),
				tel: $tr.find('td.tel').text(),
				email: $tr.find('td.email').text(),
			};
			$modal.param.managerInfo = managerInfo;
			$modal_root.data('modal-data').click_confirm_button();
		});

	// 저장
	$modal.confirm_modal = async function (e) {
		$modal_root.modal('hide');
		return $modal.param;
	};

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
			initPage($modal);
		}
	}
});
