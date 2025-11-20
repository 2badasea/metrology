$(function () {
	console.log('++ common.js');

	if ($('.modal-dialog').length > 0) {
		modal_draggable();
	}

	$('input[type=text]').attr('autocomplete', 'off'); // input창 자동완성 제거
	$('input[type=password]').attr('autocomplete', 'new-password'); // 비밀번호 항목 자동완성 제거
})
	// 0이상의 정수만 입력 가능
	.on('input', 'input.number_integer', function () {
		let this_value = this.value.replace(/\D+/g, ''); // 숫자 외 제거
		this.value = this_value;

		// NOTE 'keyup' 대신 'input'을 사용하는 이유
		// 1. keyup의 경우, '키를 뗐을 때'만 발생. 즉, 키보드 입력에만 의존.
		// 2. input의 경우, 값이 바뀌면 무조건 발생.
	})
	// 사업자번호 항목 입력 시, 포맷팅
	.on('keyup', 'input.agent_num', (e) => {
		let agent_num = e.target.value; // 화살표함수가 아닌 'this.value' 형태로도 값을 얻을 수 있음.
		agent_num = agent_num.replace(/\D/g, '').slice(0, 10);

		let out = '';
		if (agent_num.length <= 3) {
			out = agent_num;
		} else if (agent_num.length <= 5) {
			out = agent_num.slice(0, 3) + '-' + agent_num.slice(3);
		} else {
			out = agent_num.slice(0, 3) + '-' + agent_num.slice(3, 5) + '-' + agent_num.slice(5);
		}
		e.target.value = out;
	})
	// 휴대전화 번호 입력 필드
	.on('keyup', 'input.hp', (e) => {
		let contactNum = e.target.value; // 화살표함수가 아닌 'this.value' 형태로도 값을 얻을 수 있음.
		contactNum = contactNum.replace(/\D/g, '').slice(0, 11);

		let out = '';
		if (contactNum.length <= 3) {
			out = contactNum;
		} else if (contactNum.length <= 7) {
			out = contactNum.slice(0, 3) + '-' + contactNum.slice(3);
		} else {
			out = contactNum.slice(0, 3) + '-' + contactNum.slice(3, 7) + '-' + contactNum.slice(7);
		}
		e.target.value = out;
	})
	// 전화번호 입력 필드
	.on('keyup', 'input.tel', (e) => {
		let contactNum = e.target.value; // 화살표함수가 아닌 'this.value' 형태로도 값을 얻을 수 있음.
		contactNum = contactNum.replace(/\D/g, '').slice(0, 11);

		let out = '';
		if (contactNum.length <= 3) {
			out = contactNum;
		} else if (contactNum.length <= 7) {
			out = contactNum.slice(0, 3) + '-' + contactNum.slice(3);
		} else {
			out = contactNum.slice(0, 3) + '-' + contactNum.slice(3, 7) + '-' + contactNum.slice(7);
		}
		e.target.value = out;
	})
	.on('keyup', 'input.tel', (e) => {
		// 1) 숫자만 남기고, 최대 11자리까지 자르기
		let digits = e.target.value.replace(/\D/g, '');

		let out = '';

		// 2) 02 로 시작하는 경우 (서울 지역번호)
		if (digits.startsWith('02')) {
			// 02 + 8자리까지 → 최대 10자리 (02-1234-5678)
			digits = digits.slice(0, 10);
			const len = digits.length;

			if (len <= 2) {
				// 0, 02 입력 중
				out = digits;
			} else if (len <= 5) {
				// 02-123 / 02-1234 입력 중
				out = digits.slice(0, 2) + '-' + digits.slice(2);
			} else {
				// 02-123-4567 (9자리) / 02-1234-5678 (10자리)
				// -> 2 - (len-6) - 4 패턴
				out =
					digits.slice(0, 2) + // 02
					'-' +
					digits.slice(2, len - 4) + // 가운데 3~4자리
					'-' +
					digits.slice(len - 4); // 마지막 4자리
			}

			// 3) 그 외(010, 053, 031, 070 등 3자리 시작 번호)
		} else {
			// 휴대폰까지 고려해서 최대 11자리
			digits = digits.slice(0, 11);
			const len = digits.length;

			if (len <= 3) {
				// 0, 010, 053 입력 중
				out = digits;
			} else if (len <= 7) {
				// 010-123 / 053-1234 입력 중
				out = digits.slice(0, 3) + '-' + digits.slice(3);
			} else {
				// 010-1234-5678 (11자리)
				// 053-123-4567 / 053-1234-5678 (10~11자리)
				// -> 3 - (len-7) - 4 패턴
				out =
					digits.slice(0, 3) + // 앞 3자리
					'-' +
					digits.slice(3, len - 4) + // 가운데 3~4자리
					'-' +
					digits.slice(len - 4); // 마지막 4자리
			}
		}

		e.target.value = out;
	})
	// 로그아웃 이벤트 정의
	.on('click', '.logoutBtn', async function (e) {
		e.preventDefault();
		console.log('로그아웃 호출!');

		// await문에 then메서드등을 달지 않기
		const confirm_check = await Swal.fire({
			title: '로그아웃 하시겠습니까?',
			showDenyButton: true,
			showCancelButton: false,
			confirmButtonText: '네',
			denyButtonText: `아니오`,
		});

		if (!confirm_check.isConfirmed) {
			return false;
		}

		try {
			// POST 호출 (g_ajax 내부가 POST 기본이면 데이터는 빈 객체여도 OK)
			const res = await g_ajax('/logout', {}); // 서버에서 200/204/302 상관없음
			if (res?.ok != undefined && res.ok === true) {
				location.href = res.redirect;
			}
		} catch (err) {
			console.error(err);
			g_toast('로그아웃에 실패했습니다.', 'error');
		}
	});

/**
 * 모달이 아니고 페이지일 경우 페이지 자신 js를 다 수행 후 이 함수를 실행해서 init_modal 함수를 실행해야 한다.
 * 하지만 프로젝트 내부에서 통상적으로 page cotent를 감싸고 있는 부분을 $modal로 쓰기 때문에 $modal로 쓴다
 * @param {jquery} $modal
 * @param {object} param
 */
function init_page($modal, param = {}) {
	$('body').height($(window).height());
	$('.modal-view').height($(window).height() - $('.card-header.bg-dark.text-white').height());
	//부트스트랩 모달의 FocusTrap 무력화(모달 밖의 요소로 포커스가 이동하면 포커스를 탈취)
	$.fn.modal.Constructor.prototype._initializeFocusTrap = function () {
		return {
			activate: function () {},
			deactivate: function () {},
		};
	};
	let modal_script = undefined != $modal ? $modal.data('modal-data') : undefined;
	if (typeof modal_script == 'object' && typeof modal_script.title == 'string') {
		$('.main-body').find('.card-header .card-title .card-title-text').text(modal_script.title);
	}
	if (typeof modal_script == 'object' && typeof modal_script.init_modal == 'function') {
		modal_script.init_modal(param);
	}
}

/**
 * Ajax 요청을 보낸다. url, dataobject, options
 * @param {string} url
 * @param {object} data
 * @param {object} options
 * @returns primise
 */
function g_ajax(url, data = {}, options = {}) {
	// 기본적으로 post요청과 응답형식은 json으로 고정한다.
	let settings = $.extend(
		{
			url: url,
			type: 'post',
			dataType: 'json',
			data: data,
		},
		options
	);
	// data가 FormData객체라면 파일업로드 데이터를 처리하기 위해 아래 옵션 설정
	if (data instanceof FormData) {
		settings.processData = false;
		settings.contentType = false;
	}

	let error = function (request, status, error) {
		console.log(request, status, error);
		if ('undefined' != typeof options.error) {
			options.error(request, status, error);
		}
	};
	settings.error = error;
	return $.ajax(settings);
}

/**
 * 토스트 메시지를 띄운다. (메시지, 타입(info, warning, success, error), 추가설정)
 * @param {string} text
 * @param {string} type
 * @param {object} options
 */
function g_toast(text = '알림', type = 'info', options = {}) {
	if (type != 'info' && type != 'warning' && type != 'success' && type != 'error') {
		//허용되지 않은 타입일경우 info로 강제로 설정한다.
		type = 'info';
	}

	let settings = $.extend(
		{
			closeButton: false,
			debug: false,
			newestOnTop: false,
			progressBar: false,
			positionClass: 'toast-top-right',
			preventDuplicates: false,
			onclick: null,
			showDuration: '2000',
			hideDuration: '1000',
			timeOut: '5000',
			extendedTimeOut: '1000',
			showEasing: 'swing',
			hideEasing: 'linear',
			showMethod: 'fadeIn',
			hideMethod: 'fadeOut',
		},
		options
	);
	toastr.options = $.extend(toastr.options, settings);
	toastr[type](text);
	// }
	// });
	var message = g_get_data('message');
	if (message == null) {
		message = [];
	}
	message.push({
		text: text,
		type: type,
	});
	g_set_data('message', message);
}

/**
 * 브라우저 스토리지에서 데이터를 가져온다.
 *
 * @param {string} key 데이터를 관리할 키 값
 * @returns {json}
 */
function g_get_data(key, storage = localStorage) {
	try {
		var value = JSON.parse(storage.getItem(key));
	} catch (e) {
		var value = [];
	}
	return value;
}

/**
 * 브라우저 스토리지에 데이터를 저장한다.
 *
 * @param {string} key 데이터를 관리할 키 값
 * @param {mixed} value 저장할 데이터(array / object)
 */
function g_set_data(key, value, storage = localStorage) {
	let data = JSON.stringify(value);
	storage.setItem(key, data);
}

/**
 * 비동기 통신 에러에 대한 응답 처리
 *
 * @param   {[type]}  err  [err description]
 *
 * @return  {[type]}       [return description]
 */
function custom_ajax_handler(err) {
	// jQuery XHR 스타일을 최대한 커버
	const xhr = err?.xhr || err; // 커스텀 구현에 따라 다름
	const status = xhr?.status;
	const respJSON = xhr?.responseJSON;
	const respText = xhr?.responseText;

	// 옵셔널체이닝 문법(null/undefined이면 에러를 발생시키지 않고, undefined를 반환.)
	if (respJSON?.code != undefined && respJSON?.msg != undefined) {
		g_toast(respJSON.msg, 'error');
		return false;
	}

	// 서버가 JSON으로 { message: "..."} 내려주는 경우
	const msgFromJson = respJSON?.message || respJSON?.error || respJSON?.detail;

	// 텍스트 응답에서 메시지 추출
	const msgText = typeof respText === 'string' && respText.length < 300 ? respText : null;

	const message = msgFromJson || msgText || xhr?.statusText || err?.message || '요청 처리 중 오류가 발생했습니다.';
	console.log('🚀 ~ custom_ajax_handler ~ message:', message);

	// 상태코드가 있으면 붙여주면 디버깅 편함
	const label = status ? `[${status}] ${message}` : message;

	g_toast(label, 'error');
}

/**
 * php uniquid와 유사한 id 만드는 것으로
 * @param {string} prefix
 * @param {boolean} random
 * @returns
 */
function g_uniqid(prefix = '', random = false) {
	const sec = Date.now() * 1000 + Math.random() * 1000;
	const id = sec.toString(16).replace(/\./g, '').padEnd(14, '0');
	return `${prefix}${id}${random ? `.${Math.trunc(Math.random() * 100000000)}` : ''}`;
}

/**
 * 새 모달을 띄운다.
 * @param {string} url 모달로 띄울 url 주소
 * @param {object} param 해당 모달을 요청할 때 전송할 데이터
 * @param {object} options 모달 설정값
 * @return {promise}
 */
async function g_modal(url, param = {}, options = {}) {
	return new Promise(function (resolve) {
		let settings = $.extend(
			{
				uuid: g_uniqid(''),
				title: '',
				size: 'xl', //fullscreen, sm, lg, xl(default is xl)
				show_close_button: false, //닫기 버튼을 보여줄지
				close_button_class_name: 'btn btn-secondary btn-sm', //닫기 버튼 클래스
				close_button_text: '닫기', //닫기 버튼 텍스트
				click_close_button: async function () {
					let $modal = $(`#${uuid}`).find('.modal-view').data('modal-data');
					if (typeof $modal == 'object' && typeof $modal.close_modal == 'function') {
						await $modal.close_modal();
						resolve();
					} else {
						$(`#${uuid}`).modal('hide');
						resolve();
					}
				},
				show_confirm_button: false, //확인 버튼을 보여줄지
				confirm_button_class_name: 'btn btn-primary btn_save btn-sm', //확인 버튼 클래스
				confirm_button_text: '확인', //확인 버튼 텍스트
				click_confirm_button: async function () {
					let $modal = $(`#${uuid}`).find('.modal-view').data('modal-data');
					if (typeof $modal == 'object' && typeof $modal.confirm_modal == 'function') {
						let value = await $modal.confirm_modal();
						if (value !== false) {
							resolve(value);
						}
					} else {
						$(`#${uuid}`).modal('hide');
						resolve();
					}
				},
				show_reset_button: false, //초기화 버튼을 보여줄지
				reset_button_text: '초기화', //초기화 버튼 텍스트
				click_reset_button: function () {
					$(`#${uuid} .modal-body`)
						.off('load')
						.load(
							url,
							$.extend(
								{
									renderMode: 'modal',
								},
								param
							),
							function () {
								if ((match = url.match(/^\/?(.+)\/(.+)/i))) {
									let element = document.createElement('script');
									element.setAttribute('src', `/public/js/${match[1]}/${match[2]}.js`);
									document.querySelector(`#${uuid} .modal-body`).appendChild(element);
								}
							}
						);
				},
				show_select_button: false,
				select_button_text: '선택',
				select_button_icon: 'bi bi-check-square',
				select_button_class_name: 'btn btn_sub_add btn-info btn_check_select',
				click_select_button: async function () {
					let $modal = $(`#${uuid}`).find('.modal-view').data('modal-data');
					if (typeof $modal == 'object' && typeof $modal.select_modal == 'function') {
						let value = await $modal.select_modal();
						if (value !== false) {
							resolve(value);
						}
					} else {
						$(`#${uuid}`).modal('hide');
						resolve();
					}
				},
				height: undefined,
				button_area_html: '', //버튼영역 HTML 직접 입력
				custom_btn_html_arr: [], //필요한 버튼을 추가로 푸터영역 앞쪽에 넣는배열
				close_with_esc: true,
				type: 'normal',
				icon: '',
				backdrop: 'static',
				top: 0,
				left: 0,
				max_height: '100%',
				width: '',
			},
			options
		);
		const uuid = settings.uuid;
		if (settings.button_area_html == '') {
			// custom 버튼 영역부터 삽입
			for (let i = 0; i < settings.custom_btn_html_arr.length; i++) {
				settings.button_area_html += settings.custom_btn_html_arr[i];
			}

			settings.button_area_html +=
				settings.show_guide_button == true
					? settings.guide_button_html
						? settings.guide_button_html
						: `<button type="button" class="${settings.guide_button_class_name} modal-btn-guide"><i class="bi bi-guide"></i>${settings.guide_button_text}</button>`
					: '';
			settings.button_area_html +=
				settings.show_select_button == true
					? `<button type="button" class="${settings.select_button_class_name} modal-btn-select"><i class="${settings.select_button_icon}"></i>${settings.select_button_text}</button>`
					: '';
			settings.button_area_html +=
				settings.show_reset_button == true
					? `<button type="button" class="btn btn-danger btn-sm px-2 py-1 modal-btn-reset"><i class="bi bi-x-diamond"></i>${settings.reset_button_text}</button>`
					: '';
			settings.button_area_html +=
				settings.show_confirm_button == true
					? `<button type="button" class="${settings.confirm_button_class_name} modal-btn-confirm"><i class="bi bi-save"></i>${settings.confirm_button_text}</button>`
					: '';
			settings.button_area_html +=
				settings.show_close_button == true
					? `<button type="button" class="${settings.close_button_class_name} modal-btn-close" data-dismiss="modal"><i class="bi bi-x-square"></i>${settings.close_button_text}</button>`
					: '';
		}
		let match = url.match(/^\/?(.+)\/(.+)/i);
		match = null != match ? match : [];
		match[0] = undefined != match[0] ? match[0] : url;
		match[1] = undefined != match[1] ? match[1] : '';
		match[2] = undefined != match[2] ? match[2] : '';
		let modal_body_style = '';
		if (settings.height != undefined) {
			if (typeof settings.height == 'number') {
				modal_body_style += 'height: ' + settings.height + 'px;';
				modal_body_style += 'max-height: ' + settings.height + 'px;';
			} else {
				modal_body_style += 'height: ' + settings.height + ';';
				modal_body_style += 'max-height: ' + settings.height + ';';
			}
		}

		// let modal_scrollable = "";
		// if (settings.modal_scrollable != undefined) {
		// 	if (settings.modal_scrollable == true) {
		// 		modal_scrollable = "modal-dialog-scrollable";
		// 	}
		// }

		let keyboard = settings.close_with_esc ? ' data-keyboard="true"' : ' data-keyboard="false"';
		settings.icon =
			settings.type == 'help_doc' && '' == settings.icon ? '<i class="bi bi-question-circle modal_icon help_doc_icon"></i>' : settings.icon;
		let modal = `
		<div class="modal fade draggable modal-${match[1]}-${match[2]}" id="${uuid}" tabindex="-1" data-backdrop="${
			typeof settings.backdrop != 'undefined' ? settings.backdrop : 'static'
		}" role="dialog" aria-hidden="true"${keyboard} style="left: ${settings.left}px; top: ${settings.top}px;">
			<div class="modal-dialog${settings.size == 'fullscreen' ? ' modal-fullscreen ' : ' '} modal-${
			['', 'sm', 'lg', 'xl', 'xxl', 'xxxl'].indexOf(settings.size) > -1 ? settings.size : 'xl'
		} modal-dialog-centered modal-dialog-scrollable" role="document" style="${settings.width ? '--bs-modal-width: ' + settings.width + ';' : ''}">
				<div class="modal-content shadow-4" style="max-height: ${settings.max_height};">
					<div class="modal-header p-2 px-3">
						<h5 class="modal-title">${settings.icon}${settings.title}</h5>
						<div class="btn-group">
							<i class="bi bi-x-lg close"></i>
						</div>
					</div>
					<div class="modal-body modal-view scroll-x scroll-y" style="${modal_body_style}" class='shadow' data-uuid="${uuid}">
					</div>
					<div class="modal-footer p-1 ${settings.button_area_html ? '' : 'd-none'}">
						${settings.button_area_html}
					</div>
				</div>
			</div>
		</div>`;
		$(modal).appendTo('body');
		$(`#${uuid}`).on('show.bs.modal', function (e) {
			// 모달 초기설정 코드는 최초 뜰 때만 호출되어야 한다
			if (undefined == $(`#${uuid} .modal-body`).data('param')) {
				param.modal_id = uuid;
				$(`#${uuid} .modal-body`).data('param', param);
				// console.log($(`#${uuid} .modal-body`).data("param"));
				$(`#${uuid} .modal-body`).data('settings', settings);
				$(`#${uuid} .modal-body`).load(
					url,
					$.extend(
						{
							uuid: uuid,
							renderMode: 'modal',
						},
						param
					),
					function () {
						if ((match = url.match(/^\/?(.+)\/(.+)/i))) {
							// $(`#${uuid}`).trigger("modal_ready", param);
							let $modal = $(`#${uuid} .modal-body`);
							$modal.data('modal-data');
						}
						$(`#${uuid} .modal-btn-close, #${uuid} .close`).on('click', settings.click_close_button);
						$(`#${uuid} .modal-btn-confirm`).on('click', settings.click_confirm_button);
						$(`#${uuid} .modal-btn-reset`).on('click', settings.click_reset_button);
						$(`#${uuid} .modal-btn-select`).on('click', settings.click_select_button);
						$(`#${uuid}`).data('modal-url', url).data('modal-data', settings);
						// g_datepicker($(`#${uuid} .hub_input_group .datepicker`));
						// init_tagify($(`#${uuid}`));
						// g_timepicker() 함수는 개별로 따로 처리하라
						// init_scrollbar(`#${uuid} .modal-body`);
						if (typeof settings.on_load_complete == 'function') {
							settings.on_load_complete($(`#${uuid}`));
						}
					}
				);
				if (typeof settings.on_show == 'function') {
					settings.on_show($(`#${uuid}`));
				}
			}
		});
		$(`#${uuid}`)
			.modal('show')
			// .draggable({
			// 	handle: '.modal-header',
			// })
			.on('hidden.bs.modal', function () {
				$(this).remove();
				if ($('.modal-stack').length) {
					$('body').addClass('modal-open');
				}
			})
			.on('shown.bs.modal', function () {
				console.log('shown !!!!!!!!!!!!!!!!!!');
				// 띄우려는 모달의 호출자가 모달일 경우 자식이 뜨고 난 이후에 esc 키를 입력하면 부모가 닫기는 문제
				// show에서 처리하려고 했으나 처리가 안되서 shown에서 처리하는 것으로 타협
				let $btn_close = $(this).find('.modal-content button.close');
				$btn_close.attr('tabindex', -1).focus();
				$btn_close.removeAttr('tabindex');
				if (typeof settings.on_shown == 'function') {
					settings.on_shown($(`#${uuid}`));
				}
			});
	}).catch((error) => {
		console.log('Promise Error: ', error);
		return false;
	});
}

/**
 * 모달 드래그 기능 추가
 *
 * @return  {[type]}  [return description]
 */
function modal_draggable() {
	var modal_dialog = $('.modal-dialog');
	modal_dialog.draggable({
		handle: '.modal-header',
		drag: function (event, ui) {},
	});
}

/**
 * 카카오 도로명주소 API
 *
 * @param   {[type]}  zipCodeColName  우편번호
 * @param   {[type]}  addrColname     주소 DB컬럼명
 *
 * @return  {[type]}                  [return description]
 */
function sample4_execDaumPostcode(zipCodeColName, addrColname) {
	new daum.Postcode({
		oncomplete: function (data) {
			// 우편번호
			$(`input[name=${zipCodeColName}]`).val(data.zonecode);
			// 도로명 및 지번주소
			$(`.${addrColname}`).val(data.roadAddress);
		},
	}).open();
}

// 입력값 체크
/**
 * [check_input description]
 *
 * @param   {[string]}  value
 *
 * @return  {[boolean]}
 */
function check_input(value) {
	return value != null && String(value).replace(/\s+/g, '') !== '';
}

/**
 * 이메일 정규식 체크
 *
 * @param   {[string]}  value  [value description]
 *
 * @return  {[boolean]}         [return description]
 */
function check_email_reg(value) {
	return value != null && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value).trim());
}
