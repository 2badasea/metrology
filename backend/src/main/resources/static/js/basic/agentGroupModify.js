$(function () {
	console.log('++ basic/agentGroupModify.js');

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

        // 기존에 존재하는 그룹항목 select/option으로 세팅
        g_ajax('/api/basic/getGroupName', {}, {
            success: function (resData) {
                
            },
            error: function (xhr) {
                custom_ajax_handler(xhr);
            },
            complete: function (data) {

            }

        })

        
	};

	// $modal
    //     .on('click')

	// 저장
	$modal.confirm_modal = async function (e) {
        console.log('저장 진행!!');

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
