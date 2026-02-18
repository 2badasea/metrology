$(function () {
	console.log('++ guide/testIntro.js');

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

	$modal.init_modal = (param) => {
		$modal.param = param;
	};

	$modal
		// 테스트 로그인 버튼 클릭
		.on('click', '.testerLogin', async function () {
			$modal.param.useAutoLogin = true;
			$modal.param.username = 'tester';
			$modal.param.password = '123456';
			$modal_root.data('modal-data').click_return_button();
		});

	// 닫기 클릭 시
	$modal_root.on('click', '.modal-btn-close', function () {
		const isChecked = $('#testerIntroHideToday', $modal_root).prop('checked');
		// 체크 상태인 경우 로컬 스토리지에 값 담기
		const LC_IS_USE_TESTER = 'isUseTester';
		const getTodayYmd = () => {
			const d = new Date();
			const y = d.getFullYear();
			const m = String(d.getMonth() + 1).padStart(2, '0');
			const day = String(d.getDate()).padStart(2, '0');
			return `${y}-${m}-${day}`;
		};

		if (isChecked) {
			const today = getTodayYmd();
			localStorage.setItem(LC_IS_USE_TESTER, today);
		} else {
			// 체크 안 했으면 저장 안 하거나, 기존 값 제거(선택)
			// localStorage.removeItem(LC_IS_USE_TESTER);
		}
		$modal_root.modal('hide');
	});

	$modal.return_modal = () => {
		$modal_root.modal('hide');
		return $modal.param;
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
			initPage($modal);
		}
	}
});
