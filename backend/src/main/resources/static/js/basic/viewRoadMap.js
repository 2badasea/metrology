$(function () {
	console.log('++ basic/viewRoadMap.js');

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
		const address = $modal.param.address;

		const geocoder = new kakao.maps.services.Geocoder();
		geocoder.addressSearch(address, function (result, status) {
			if (status !== kakao.maps.services.Status.OK || !result.length) {
				// 로드뷰 표시 불가 시 지도 표시 등 fallback 권장
				gToast('로드뷰 표시가 불가한 주소입니다.', 'warning');
                // 약간의 지연시간을 두고 진행할 것
				setTimeout(() => {
					$modal_root.modal('hide');
				}, 500);
				return false;
			}

			const x = parseFloat(result[0].x);
			const y = parseFloat(result[0].y);

			const position = new kakao.maps.LatLng(y, x);
			const roadviewContainer = document.getElementById('roadview');
			const roadview = new kakao.maps.Roadview(roadviewContainer);
			const roadviewClient = new kakao.maps.RoadviewClient();

			roadviewClient.getNearestPanoId(position, 50, function (panoId) {
				if (!panoId) {
					return false; // 해당 위치 주변에 로드뷰가 없을 수 있음
				}
				roadview.setPanoId(panoId, position);
			});
		});
	};

	// 저장
	// $modal.confirm_modal = async function (e) {
	// };

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
			initPage($modal);
		}
	}
});
