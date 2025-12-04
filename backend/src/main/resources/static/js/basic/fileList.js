$(function () {
	console.log('++ basic/fileList.js');

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
		console.log('🚀 ~ $modal.param:', $modal.param);

	};


	// 저장?
	$modal.confirm_modal = async function (e) {

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
			init_page($modal);
		}
	}
});
