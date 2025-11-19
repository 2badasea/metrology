$(function () {
	console.log('++ member/memberJoin.js');

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
			const loginId = $('input[name=loginId]', $modal).val();
			if (!loginId || loginId.trim().length === 0) {
				g_toast('사업자번호를 입력하세요', 'warning');
				return false;
			}
			$(this).prop('disabled', true);

			// Swal.fire({
			// 	title: '중복체크 진행 중...',
			// 	html: '',
			// 	allowOutsideClick: false,
			// 	didOpen: () => {
			// 		Swal.showLoading(); // 로딩창 표시
			// 	},
			// });

			try {
				// 사업자번호지만, 로그인ID로 사용되기 때문에 loginId 키값으로 보낸다.
				const res = await g_ajax('/apiMember/chkDuplicateLoginId', {
					loginId: loginId,
					refPage: 'memberJoin',
				});
				console.log('🚀 ~ res:', res);

				if (!res || res?.code == undefined) {
					throw new Error('응답 형식이 올바르지 않습니다.');
				}
				// code, msg, data
				let msg = res.msg;
				let code = res.code;
				let data = res.data;

				if (data.agentName || data.agentAddr) {
					msg += `\n업체명: ${data.agentName}, 업체주소: ${data.agentAddr}`;
				}
				g_toast(msg, code == 1 ? "success" : "warning");
				// TODO 중복이 없다면, 내부적으로 중복체크를 완료했다는 flag 심기

			} catch (err) {
				console.log('🚀 ~ err:', err);
				custom_ajax_handler(err);
			} finally {
				// 중복체크 버튼 disable 풀기
				$(this).prop('disabled', false);
			}

			// 중복통과 시, 버튼색상변경 및 값 설정
		})
		// 사업자번호 항목에 keyup 이벤트 시, 중복체크 해제
		// 메일주소 선택 이벤트
		.on('change', 'select.mailSelect', function () {
			const emailInput = $('.emailInput', $modal);
			const optVal = $(this).val();
			if (optVal !== 'custom') {
				emailInput.val('').prop('readonly', true);
			} else {
				emailInput.val('').prop('readonly', false);
			}
		});

	$modal.confirm_modal = async (e) => {
		console.log('가입신청!!');
		// 1. 필수입력값 요소 체크, 2. 비밀번호 확인이 되었는지. 3. 사업자번호 중복체크 진행했는지 확인 4. 개인정보 약관 동의 체크
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
		// $modal_root.on('modal_ready', function (e, p) {
		// 	console.log("이벤트체크");
		// 	$modal.init_modal(p);
		// if (typeof $modal.grid == 'object') {
		// 	$modal.grid.refreshLayout();
		// }
		// });
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			init_page($modal);
		}
	}
});
