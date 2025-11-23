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
		$modal.param = param;
		console.log('🚀 ~ $modal.param:', $modal.param);
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
			const $btn = $(this);
			const loginId = $('input[name=loginId]', $modal).val();
			if (!loginId || loginId.trim().length === 0 || loginId.length != 12) {
				g_toast('사업자번호를 입력하세요', 'warning');
				return false;
			}
			// 버튼 활성화
			$btn.prop('disabled', true);

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

				let code = res.code;
				let msg = res.msg;
				let data = res.data;
				if (code != 1 && (data.agentName || data.agentAddr)) {
					msg += `\n업체명: ${data.agentName}, 업체주소: ${data.agentAddr}`;
				}
				if (code == 1) {
					$btn.val('y').removeClass('btn-secondary').addClass('btn-success');
				} else {
					$btn.val('n').removeClass('btn-success').addClass('btn-secondary');
				}
				g_toast(msg, code == 1 ? 'success' : 'warning');
			} catch (err) {
				console.log('catch문 에러 내용 확인 err:', err);
				custom_ajax_handler(err);
			} finally {
				// 중복체크 버튼 disable 풀기
				$btn.prop('disabled', false);
			}
		})
		// 사업자번호 항목에 keyup 이벤트 시, 중복체크 해제
		.on('keyup', 'input[name=loginId]', function (e) {
			// 엔터키 입력 시, 중복체크 트리거
			if (e.keyCode === 13) {
				$('button.check_agent_num', $modal).trigger('click');
			} else {
				// 값 비활성화
				$('button.check_agent_num', $modal).val('n').removeClass('btn-success').addClass('btn-secondary');
			}
		})
		// 메일주소 선택 이벤트
		.on('change', 'select.mailSelect', function () {
			const emailInput = $('.emailInput', $modal);
			const optVal = $(this).val();
			if (optVal !== 'custom') {
				emailInput.val('').prop('readonly', true);
			} else {
				emailInput.val('').prop('readonly', false);
			}
		})
		// 비밀번호 확인 입력 이벤트 (실시간 검증)
		.on('keyup', '.confirmPassword', function () {
			const pwdValue = $('input[name=pwd]', $modal).val();
			const chkValue = $(this).val();
			// 일치하지 않을 경우, 메시지 노출
			if (chkValue !== pwdValue) {
				$('.chkPwdMsg', $modal).removeClass('d-none');
			} else {
				$('.chkPwdMsg', $modal).addClass('d-none');
			}
		})
		// 대표자(명)과 이름(담당자)명과 동일한 경우
		.on('change', '.checkIsSameManager', function () {
			const isChecked = $(this).is(':checked'); // prop('checked')로 체크 가능
			// 체크된 상태라면
			if (isChecked) {
				const managerName = $('input[name=manager]', $modal).val();
				$('input[name=ceo]', $modal).val(managerName);
			}
		});

	// 가입신청
	$modal.confirm_modal = async function (e) {
		console.log('가입신청!!');

		const $btn = $('.btn_save', $modal);
		$btn.prop('disabled', true);
		const $form = $('.memberJoinForm', $modal);
		// formdata 객체로 서버에 요청을 보내는 경우, 인식하지 못하여 JSON 형태로 변경
		// const formData = new FormData($form[0]);
		const memberJoinData = $form.serialize_object();

		// 필수입력값 체크
		const $chkInputs = $('input[name!=""]', $form);

		let flagForm = true;
		let chkMsg = '';
		$.each($chkInputs, function (index, ele) {
			const name = $(ele).attr('name');
			const value = $(ele).val();

			// [필수]사업자번호 체크
			if (name === 'loginId') {
				const btnVal = $('.check_agent_num', $form).val();
				if (btnVal !== 'y') {
					chkMsg = '사업자번호 중복체크를 해주세요.';
					flagForm = false;
				}
			}

			// [필수]업체명 확인
			if (name === 'name') {
				if (!check_input(value)) {
					chkMsg = '업체명을 입력해주세요.';
					flagForm = false;
				}
			}

			// 주소(값 구성 필요)
			if (name === 'addr') {
				const detailAddr = $('.addr2', $form).val();
				// append의 경우, key에 해당 값을 추가하게 됨
				// formData.set('addr', `${value} ${detailAddr}`);
				// formData.set('addr', `${value} ${detailAddr}`);
				memberJoinData.addr = `${value} ${detailAddr}`;
			}

			// [필수]비밀번호
			if (name === 'pwd') {
				if (!check_input(value)) {
					chkMsg = '비밀번호를 입력해주세요.';
					flagForm = false;
				} else {
					const chkPwd = $('.confirmPassword', $form).val();
					if (value !== chkPwd || !check_input(chkPwd)) {
						chkMsg = '비밀번호 확인을 해주세요.';
						flagForm = false;
					}
				}
			}

			// 이름
			if (name === 'manager') {
				if (!check_input(value)) {
					chkMsg = '이름(담당자)를 입력해주세요.';
					flagForm = false;
				}
			}

			// 이메일
			if (name === 'email') {
				const optVal = $('.mailSelect', $form).val();
				let mailDomain = '';
				if (!optVal) {
					chkMsg = '이메일 도메인주소를 선택/입력해주세요.';
					flagForm = false;
				} else {
					mailDomain = (optVal === 'custom') ? $('.emailInput', $form).val() : optVal;
				}

				const email = `${value}@${mailDomain}`;
				console.log("🚀 ~ email:", email);
				// 정규식 체크
				if (!check_email_reg(email)) {
					chkMsg = '이메일 형식이 올바르지 않습니다.';
					flagForm = false;
				} else {
					// formData.set('email', email);
					memberJoinData.email = email;
				}
			}

			// 휴대폰번호
			if (name === 'phone') {
				if (!check_input(value)) {
					chkMsg = '휴대폰번호를 입력해주세요.';
					flagForm = false;
				}
			}

			if (!flagForm) {
				return false;
			}
		});

		// 개인정보 처리방침 확인
		const isChecked = $('.check_privacy').is(':checked');
		if (!isChecked) {
			flagForm = false;
			chkMsg = '개인정보 처리방침에 체크해주세요.';
		}

		// 값 검증에 실패한 경우 멈춤
		if (!flagForm) {
			g_toast(chkMsg, 'warning');
			$btn.prop('disabled', false);
			return false;
		}

		// 최종 입력값 확인 formData.entires()는 formdata 전용
		// for (const [key, value] of formData.entries()) {
		// 	console.log(key, value);
		// }

		// 일반 JSON 요소 순회하기 Object.entries(obj) 형태로 사용
		console.log('입력값확인');
		for (const [key, value] of Object.entries(memberJoinData)) {
			console.log(key, value);
		}

		if (confirm('회원가입을 하시겠습니까?')) {
			// api 호출
			try {
				Swal.fire({
					title: '회원가입 신청중...',
					allowOutsideClick: false,
					didOpen: () => {
						Swal.showLoading();
					},
				}).then((result) => {});

				const res = await g_ajax('/apiMember/memberJoin', JSON.stringify(memberJoinData), {
					contentType: 'application/json;charset=utf-8',
				});
				console.log(res);
				if (!res) {
					g_toast('응답 형식이 올바르지 않습니다.', 'error');
				}
				// 가입 성공 시,
				if (res.code > 0) {
					// 가입신청이 완료되었다는 메시지와 함께 모달창이 닫히도록 한다.
					Swal.fire(res.msg ?? '회원가입 신청 성공', '', 'success').then((result) => {
						console.log(result);
						if (result.isConfirmed) {
							// NOTE confirm_modal을 통해서 값 세팅을 하는 경우, 응답이 오자마자 g_modal을 호출한 쪽의 resData가 조회
							// NOTE 리턴값이 필요한 경우에는 select_modal이나 다른 커스텀 함수 사용 필요
							// $modal.param.joinResult = 'success';		// 모달창 닫힐 때 데이터 확인
							$modal_root.modal('hide');
							// return $modal.param;
						}
					});
				} else {
					g_toast('응답 형식이 올바르지 않습니다.', 'error');
				}
			} catch (err) {
				custom_ajax_handler(err);
			} finally {
				// Swal.close();
				$btn.prop('disabled', false);
			}
		}
		// confirm 메서드 취소처리
		else {
			$btn.prop('disabled', false);
			return false;
		}

		// $modal_root.modal('hide');		// 이렇게 모달창을 닫는 경우 resData 자체를 못 받음
		// $(".modal-btn-close", $modal).trigger('click');	// 그냥 닫힘(리턴 데이터 없음)
		// $modal_root.data("modal-data").click_close_button();
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
