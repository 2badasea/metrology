$(function () {
	console.log('++ member/memberJoin.js');

	let $modal = $('.modal-view:not(.modal-view-applied)');
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = (param) => {
		console.log('> ~ param:', param);
	};

	$modal
		// 비밀번호 입력이벤트
		.on('keyup', '.confirmPassword', function (e) {
			e.preventDefault();
			const pwd = $('input[name=pwd]', $modal).val();
		})
		// 사업자번호 중복체크
		.on('click', '.check_agent_num', async function (e) {
			e.preventDefault();
			const $agent_num = $('input[name=login_id]', $modal).val();
			if (!$agent_num || $agent_num.trim().length === 0) {
				g_toast('사업자번호를 입력하세요', 'warning');
				return false;
			}

			Swal.fire({
				title: '중복체크 진행 중...',
				html: '',
				allowOutsideClick: false,
				didOpen: () => {
					Swal.showLoading(); // 로딩창 표시
				},
			});

			try {
				const res = await g_ajax('/basic/isDuplicateAgentNum', {
					agentNum: $agent_num, // 서버에 데이터를 보낼 때는 java형식에 맞게 카멜케이스로 보낸다.
				});
				console.log('🚀 ~ res:', res);

				if (!res || res?.code == undefined) {
					throw new Error('응답 형식이 올바르지 않습니다.');
				}
			} catch (err) {
				console.log('🚀 ~ err:', err);
				custom_ajax_handler(err);
			} finally {
			}

			// 중복통과 시, 버튼색상변경 및 값 설정
		});
	// 사업자번호 항목에 keyup 이벤트 시, 중복체크 해제

	$modal.confirm_modal = async (e) => {
		console.log('가입신청!!');
		// 1. 필수입력값 요소 체크, 2. 비밀번호 확인이 되었는지. 3. 사업자번호 중복체크 진행했는지 확인 4. 개인정보 약관 동의 체크
	};

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
