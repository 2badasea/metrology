$(function () {
	console.log('++ member/login.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		// 이번 memberJoin 모달의 body
		$modal = $bodyCandidate.first();
	} else {
		// 페이지로 직접 열렸을 수도 있으니, 그때는 그냥 첫 번째 modal-view 사용
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	$modal.init_modal = (param) => {
		console.log('🚀 ~ param:', param);
	};

	// 페이지에 대한 이벤트
	$modal
		// 로그인 시도
		.on('click', '.login_btn', async function (e) {
			e.preventDefault();

			// NOTE 1. POST방식 요청, 2. JSON형태의 응답, 3. try/catch 사용 4. sweet alert 요청/응답
			const $form = $('.login_form', $modal);

			const username = $('input[name=username]', $form).val().trim();
			const password = $('input[name=password]', $form).val().trim();

			if (username.length === 0) {
				g_toast('아이디를 입력해주세요.', 'warning');
				return false;
			}
			if (password.length === 0) {
				g_toast('비밀번호를 입력해주세요.', 'warning');
				return false;
			}

			// 로그인 버튼 비활성화
			$(this).prop('disabled', true);

			// 비동기 요청 자체를 try/catch로 감싸준다. (네트워크에러, 서버 4xx/5xx, JSON파싱 실패 등의 이유로 Promise가 reject되면 흐름이 깨지기 때문)
			try {
				// promise 객체를 반환하는 형태의 비동기요청은 success/error 옵션은 빼는 게 깔끔.
				// 콜백 옵션을 넘기면서 await까지 쓴느 건 이중 구조
				// json 데이터를 넘길 때, key:value명이 동일하다면 단축 표현식으로 사용 가능
				const res = await g_ajax('/apiMember/login', {
					username: username,
					password: password,
					'remember-me': $('input[name=remember-me]').val(),
				});

				// 응답 코드에 대해서 처리
				if (!res) {
					g_toast('응답 형식이 올바르지 않습니다.', 'error');
				}
				// 정상적인 응답 코드에 대한 처리
				if (res.code > 0) {
					Swal.fire(res.msg ?? '로그인 성공', '', 'success').then(() => {
						// 로그인 성공에 대한 URL 리턴 구분
						let return_url = '';
						// 일반 user
						if (res.code == 1) {
							return_url = '/basic/home';
						}
						// admin 권한을 가진 유저 (admin페이지 개발 이후 경로 변경할 것)
						else {
							return_url = '/basic/home';
						}
						location.href = return_url;
					});
				} else {
					Swal.fire(res.msg ?? '로그인 실패', '', 'warning');
					// g_toast(res.msg, 'warning');
				}
			} catch (err) {
				console.log('catch!!');
				custom_ajax_handler(err);
			} finally {
				// 로그인 버튼 비활성화 해제
				$(this).prop('disabled', false);
			}
		})
		// 로그인 이벤트 키업
		.on('keyup', 'input[name=password]', (e) => {
			if (e.keyCode == 13) {
				$('.login_btn', $modal).trigger('click');
			}
		})
		// 회원가입 모달창 띄우기
		.on('click', '.join_btn', async function () {
			await g_modal(
				'/member/memberJoin',
				{
					test: 'bada',
				},
				{
					size: 'lg',
					title: '회원가입',
					show_close_button: true,
					show_confirm_button: true,
					confirm_button_text: '가입신청',
				}
			).then(resData => {
				console.log("🚀 ~ resData:", resData);
				
			});
		});

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
