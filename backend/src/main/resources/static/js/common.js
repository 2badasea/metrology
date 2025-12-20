// FIX 그리드 선언하는 것 공통함수로 만들기
const Grid = tui.Grid;
Grid.applyTheme('custom', {
	grid: {
		border: '#004082',
	},
	frozenBorder: {
		border: '#DCE1E6',
	},
	row: {
		hover: {
			background: '#eeeeee',
		},
	},
});

$(function () {
	console.log('++ common.js');

	if ($('.modal-dialog').length > 0) {
		modal_draggable();
	}

	$('input[type=text]').attr('autocomplete', 'off'); // input창 자동완성 제거
	$('input[type=password]').attr('autocomplete', 'new-password'); // 비밀번호 항목 자동완성 제거

	// breadcumb 내 메뉴명 표시
	const flagMenuActive = $('.sidebar-menu a').hasClass('active');
	if (flagMenuActive) {
		const $activeMenu = $('.sidebar-menu').find('a.active');
		const childMenuName = $activeMenu.text();
		const parentMenuName = $activeMenu.closest('.sidebar-parent-li').find('span').eq(0).text();
		// 부모노드가 있는 경우엔 같이 표시. 없는 경우엔 현재 active 상태의 메뉴명만 표시
		const menuPath = parentMenuName != '' ? `${parentMenuName} > ${childMenuName}` : childMenuName;
		$('.topbar-inner .customBreadcrumb').text(menuPath);
	}
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
	.on('keyup', 'input.agentNum', (e) => {
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
	// 소수점 한 자리까지 입력 가능
	.on('input', 'input.numDecimal1', function () {
		let v = this.value;

		// 1) 숫자와 점(.)만 남기기
		v = v.replace(/[^0-9.]/g, '');

		// 2) 점이 여러 개면 첫 번째만 남기기
		const firstDot = v.indexOf('.');
		if (firstDot !== -1) {
			v = v.slice(0, firstDot + 1) + v.slice(firstDot + 1).replace(/\./g, '');
		}

		// 3) 소수점 이하 1자리로 제한
		if (firstDot !== -1) {
			const [intPart, decPart = ''] = v.split('.');
			v = intPart + '.' + decPart.slice(0, 1);
		}

		// ".5" 같은 형태를 "0.5"로 보정하고 싶으면
		if (v.startsWith('.')) v = '0' + v;

		this.value = v;
	})
	// 전화번호 입력 필드
	// .on('keyup', 'input.tel', (e) => {
	// 	let contactNum = e.target.value; // 화살표함수가 아닌 'this.value' 형태로도 값을 얻을 수 있음.
	// 	contactNum = contactNum.replace(/\D/g, '').slice(0, 11);

	// 	let out = '';
	// 	if (contactNum.length <= 3) {
	// 		out = contactNum;
	// 	} else if (contactNum.length <= 7) {
	// 		out = contactNum.slice(0, 3) + '-' + contactNum.slice(3);
	// 	} else {
	// 		out = contactNum.slice(0, 3) + '-' + contactNum.slice(3, 7) + '-' + contactNum.slice(7);
	// 	}
	// 	e.target.value = out;
	// })
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
			// 시큐리티 필터 체인 내부에서 로그아웃 요청에 대해 처리 후 response.setContentType과 reponse.getWriter().get()를 통해 응답 메시지 설정 후 반환
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
	// false로 설정 시, 브라우저는 헤더를 알아서 multipart/form-data로 contenType을 설정해서 보냄
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
 * <form>안의 요소들에 대해서 JSON 형태로 직렬
 * <form> 태그를 제이쿼리 선택자로 지정한 상태에서 호출 (꼭 form 태그 아니어도 됨. 호출하는 객체가 중요)
 */
$.fn.serialize_object = function () {
	var obj = {};
	try {
		this.find('input[name], select[name], textarea[name]').each(function (index, ele) {
			const name = $(ele).attr('name');
			const type = $(ele).attr('type'); // text, password, radio, checkbox ...
			let value = $(ele).val();
			if ('checkbox' == type) {
				if ('undefined' == typeof obj[name]) {
					obj[name] = [];
				}
				if ($(ele).is(':checked')) {
					obj[name].push(value);
				}
			} else if ('radio' == type) {
				if ($(ele).is(':checked')) {
					obj[name] = value;
				}
			} else {
				obj[name] = value;
			}
		});
	} catch (e) {
		console.log(e.message);
	} finally {
	}

	return obj;
};

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
	// jQuery xhr 스타일에 맞춰서 구현
	console.error('catch문!');
	console.error(err);
	const xhr = err?.xhr || err;
	const status = xhr?.status;
	const respJSON = xhr?.responseJSON;

	// 옵셔널체이닝 문법(null/undefined이면 에러를 발생시키지 않고, undefined를 반환.)
	if (respJSON?.code != undefined && respJSON?.msg != undefined) {
		g_toast(respJSON.msg, 'error');
	}
	// 다른 형식으로 받는 경우
	else {
		g_toast('요청을 처리 중 서버에서 오류가 발생했습니다. 적절한 응답 형식을 찾지 못했습니다.', 'error');
	}
	return false;

	// NOTE 아래 소스들은 우선 모두 주석처리
	const respText = xhr?.responseText;
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
				// 모달이 닫힐 때, 데이터를 반환하는 이벤트
				click_return_button: async function () {
					let $modal = $(`#${uuid}`).find('.modal-view').data('modal-data');
					if (typeof $modal == 'object' && typeof $modal.confirm_modal == 'function') {
						let value = await $modal.return_modal();
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
			// 모달창 드래그 이동 활성화
			.draggable({
				handle: '.modal-header',
			})
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
function sample4_execDaumPostcode(zipCodeColName, addrColname, addrEnColname = '') {
	return new Promise((resolve) => {
		new daum.Postcode({
			oncomplete: function (data) {
				// 우편번호
				if (zipCodeColName) {
					$(`input[name=${zipCodeColName}]`).val(data.zonecode);
				}
				// 도로명 및 지번주소
				if (addrColname) {
					$(`.${addrColname}`).val(data.roadAddress);
				}
				if (addrEnColname) {
					$(`.${addrEnColname}`).val(data.roadAddressEnglish);
				}

				resolve();
			},
		}).open();
	});
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

function g_loading_message(title) {
	Swal.fire({
		title: title,
		allowOutsideClick: false,
		showCancelButton: false,
		showConfirmButton: false,
		willOpen: () => {
			Swal.showLoading();
		},
	});
	return Swal;
}

/**
 * grid row 오브젝트에서 그리드에 종속된 속성을 제외한 나머지 속성을 복사해서 반환한다.
 * @param {object} obj
 * @returns {object}
 */
function cloneObject(obj) {
	var clone = {};
	for (var key in obj) {
		if (['rowKey', 'rowSpanMap', 'uniqueKey', 'sortKey', '_attributes', '_disabledPriority', '_relationListItemMap'].indexOf(key) === -1) {
			if (typeof obj[key] == 'object' && obj[key] != null) {
				clone[key] = cloneObject(obj[key]);
			} else {
				clone[key] = obj[key];
			}
		}
	}

	return clone;
}

/**
 * Grid를 만들어 배치한다.
 * @param {string} selector jquery 선택자
 * @param {object} options 설정(columns, data 필수)
 * @returns Grid object
 */
function g_grid(selector, options) {
	let response;
	if (options.response != undefined) {
		response = options.response;
		delete options.response;
	}
	const Grid = tui.Grid;
	let settings = $.extend(
		{
			el: typeof selector == 'string' ? $(selector)[0] : selector[0],
			language: 'ko',
			scrollX: false,
			scrollY: false,
			header: {
				height: 34,
			},
			rowHeight: 34,
			minRowHeight: 34,
			useClientSort: false,
			columnOptions: {
				// resizable: true,
				resizable: false,
			},
			pageOptions: {
				perPage: 20,
			},
			showConfigButton: true, // 모달에 설정버튼을 추가한다.
			contextMenu: null,
		},
		options
	);
	Grid.setLanguage('ko', {
		display: {
			noData: options.noData ? options.noData : '데이터가 존재하지 않습니다.',
		},
	});
	Grid.applyTheme('default', {
		grid: {
			border: '#004082',
		},
		frozenBorder: {
			border: '#DCE1E6',
		},
		outline: {
			border: custom_style.border,
			showVerticalBorder: true,
		},
		selection: {
			background: '#4daaf9',
			border: '#004082',
		},
		scrollbar: {
			background: custom_style.scrollbar_bg,
			thumb: '#d9d9d9',
			active: '#c1c1c1',
		},
		area: {
			header: {
				border: custom_style.border,
			},
		},
		cell: {
			normal: {
				background: custom_style.cell_normal_bg_bg,
				border: custom_style.border,
				showVerticalBorder: true,
				lineHeight: 'normal',
			},
			header: {
				background: custom_style.cell_hd_bg,
				border: custom_style.border,
				showVerticalBorder: true,
			},
			rowHeader: {
				border: custom_style.border,
				showVerticalBorder: true,
			},
			editable: {
				background: 'white',
				// background: '#f5f5f5',
			},
			selectedHeader: {
				background: '#d8d8d8',
			},
			focused: {
				border: custom_style.cell_focus_border,
			},
			disabled: {
				text: '#b0b0b0',
			},
			summary: {
				background: '#f7f7f7',
			},
		},
		row: {
			hover: {
				background: '#eeeeee',
			},
		},
	});
	let columns = settings.columns ?? [];
	let new_columns = [];
	let fixed = {
		size: [],
		header: [],
		hidden: [],
		align: [],
	};
	for (var i in columns) {
		if (columns[i].fixed != undefined && columns[i].fixed.size === true) {
			fixed.size.push(columns[i].name);
		}
		if (columns[i].fixed != undefined && columns[i].fixed.header === true) {
			fixed.header.push(columns[i].name);
		}
		if (columns[i].fixed != undefined && columns[i].fixed.hidden === true) {
			fixed.hidden.push(columns[i].name);
		}
		if (columns[i].fixed != undefined && columns[i].fixed.align === true) {
			fixed.align.push(columns[i].name);
		}
	}
	let defaultColumns = cloneObject(columns);

	let grid = new Grid(settings);
	grid.g_config_json = g_config_json;
	grid.g_real_postfix = g_real_postfix;
	grid.scroll_top = 0;
	grid.on('beforeRequest', function (e) {
		grid.scroll_top = $('.tui-grid-rside-area .tui-grid-body-area', grid.el)[0].scrollTop;
	});

	grid.on('response', function (e) {
		let json_row = JSON.parse(e.xhr.response);
		if (undefined != json_row && undefined != json_row.data && undefined != json_row.data.pagination) {
			let total_count = json_row.data.pagination.totalCount;
			g_append_pagenation_side_grid($(selector), `총 ${number_format(total_count)} 건`);
		}
		if (typeof response === 'function') {
			response(e);
		}
	});

	grid.on('onGridUpdated', function (e) {
		// console.log('onGridUpdated');
		// console.log(grid.scroll_top);

		$('.tui-grid-rside-area .tui-grid-body-area', grid.el)[0].scrollTop = grid.scroll_top;
	});

	grid.on('onGridMounted', function (e) {
		// console.log('onGridMounted');
		setTimeout(function (d) {
			grid.refreshLayout();
			$('.tui-grid-header-area', grid.el).on('wheel', function (e) {
				//헤더영역에서 스크롤시 좌우스크롤링 되도록 변경
				$('.tui-grid-rside-area .tui-grid-body-area', grid.el).scrollLeft(
					$('.tui-grid-rside-area .tui-grid-body-area', grid.el).scrollLeft() + e.originalEvent.deltaY
				);
			});
			let body_el = $('.tui-grid-rside-area .tui-grid-body-area', grid.el)[0];
			// console.log(body_el._listeners.wheel);
			body_el._listeners.wheel = function (ev) {
				ev.preventDefault();
			};
			body_el.addEventListener('wheel', function (e) {
				e.preventDefault();
				if (parseFloat($(body_el).offset().top) + parseFloat($(body_el).height()) - e.clientY <= 0) {
					body_el.scrollLeft += e.deltaY;
				} else {
					body_el.scrollTop += e.deltaY;
				}
			});
		}, 200);
	});

	if (settings.showConfigButton && typeof settings.config_key != 'undefined' && settings.config_key != '') {
		const modal_body = (typeof selector == 'object' ? selector : $(selector)).closest('.modal-view');
		let config_btn = '';
		if (typeof modal_body.data('config') == 'undefined') {
			modal_body.data('config', []);
			if (modal_body.hasClass('modal-body')) {
				config_btn = modal_body.prev('.modal-header').find('.config_btn').removeClass('d-none');
			} else if (modal_body.hasClass('card-body')) {
				config_btn = modal_body.prev('.card-header').find('.config_btn').removeClass('d-none');
			}
		}
		let config = modal_body.data('config');
		config.push({
			grid: grid,
			config_key: settings.config_key,
		});
		modal_body.data('config', config);
		config_btn.on('click', function () {
			let dropdown = $(this).next('.dropdown-menu');
			if ($(this).next('.dropdown-menu').find('.mCSB_container').length > 0) {
				dropdown.mCustomScrollbar('destroy');
			}
			dropdown.dropdown('toggle').html('');
			const configs = modal_body.data('config');
			for (var i in configs) {
				const { config_key, grid } = configs[i];
				let form = $('<form></form>');
				let table = $('<table class="table table-sm table-responsive-sm"></table>');
				$(`
				<colgroup>
					<col width="18px" />
					<col width="18px" />
					<col width="30%" />
					<col width="30%" />
					<col width="15%" />
					<col width="15%" />
				</colgroup>
				<tr>
					<th colspan="6" class="text-center py-1">
						<!--<button class="btn btn-primary bg-primary me-2 site_setting_save">전체저장</button>-->
						<button class="btn btn-sm btn-primary bg-primary me-2 setting_save">저장</button>
						<button class="btn btn-sm btn-secondary me-2 setting_close">닫기</button>
						<button class="btn btn-sm btn-danger bg-danger setting_reset">초기화</button>
					</th>
				</tr>`).appendTo(table);
				$(
					'<tr><th></th><th><input type="checkbox" class="form-check-input checkall" checked /></th><th class="text-center">원 항목명</th><th class="text-center">현재 항목명</th><th class="text-center">크기</th><th class="text-center">정렬</th></tr>'
				).appendTo(table);
				console.log('grid');
				console.log(grid);
				console.log(grid.getColumns());
				let columns = grid.getColumns();
				let original_columns = defaultColumns;
				let ij = 0;
				// for (var k in columns) {
				columns.forEach((row) => {
					// let row = columns[k];
					let original_name = '';
					for (var i in original_columns) {
						if (original_columns[i].name == row.name) {
							original_name = original_columns[i].header;
						}
					}
					ij++;
					let hidden = row.hidden ? '' : ' checked="checked"';
					let width = row.baseWidth != 0 ? row.baseWidth : '';
					let header = row.header.replace('<br>', ' ');
					let name = row.name;
					let is_fixed_size = fixed.size.indexOf(name) !== -1;
					let is_fixed_header = fixed.header.indexOf(name) !== -1;
					let is_fixed_hidden = fixed.hidden.indexOf(name) !== -1;
					let is_fixed_align = fixed.align.indexOf(name) !== -1;
					let left = row.align == 'left' ? ' selected="selected"' : '';
					let center = row.align == 'center' ? ' selected="selected"' : '';
					let right = row.align == 'right' ? ' selected="selected"' : '';
					$(`
					<tr class="sortable">
						<th><i class="bi bi-grip-vertical"></i></th>
						<th>
							<input type="checkbox" class="form-check-input"${hidden} name="config_hidden"${is_fixed_hidden ? ' disabled' : ''} />
							<input type="hidden" name="config_name" value="${name}" />
						</th>
						<th class="text-center">
							${original_name}
						</th>
						<td>
							<input type="text" class="p-0 form-control form-control-xs text-center config_input" value="${header}" name="config_header" autocomplete="off"${
						is_fixed_header ? ' disabled' : ''
					} />
						</td>
						<td>
							<input type="text" class="p-0 form-control form-control-xs text-center config_input" value="${width}" name="config_width" autocomplete="off" placeholder="자동"${
						is_fixed_size ? ' disabled' : ''
					} />
						</td>
						<td>
							<select name="config_align" autocomplete="off" class="p-0 form-control form-control-xs text-center config_input"${
								is_fixed_align ? ' disabled' : ''
							} style="height:auto !important">
								<option value="left"${left}>좌측</option>
								<option value="center"${center}>가운데</option>
								<option value="right"${right}>우측</option>
							</select>
						</td>
					</tr>
					`).appendTo(table);
				});
				//그리드 설정 팝업 닫기버튼
				$('button.setting_close', table).on('click', function (e) {
					e.preventDefault();
					dropdown.dropdown('toggle');
				});
				//그리드 설정 모두 선택 체크박스
				$('input.checkall', table).click(function () {
					let checked = $(this).prop('checked');
					$('input[name=config_hidden]:not(:disabled)', table).prop('checked', checked);
				});
				//
				$('input,select', table).change(function () {
					let hidden = table.find('input[name=config_hidden]');
					let header = table.find('input[name=config_header]');
					let name = table.find('input[name=config_name]');
					let width = table.find('input[name=config_width]');
					let align = table.find('select[name=config_align]');
					grid_column_config_redraw(grid, name, hidden, header, width, align);
					grid_column_config_redraw(grid, name, hidden, header, width, align); //컬럼헤더 명칭이 바로 변경되지 않아서 2번실행. 문제점 찾을것
				});
				$('button.setting_save', table).on('click', function (e) {
					e.preventDefault();
					let hidden = table.find('input[name=config_hidden]');
					let header = table.find('input[name=config_header]');
					let name = table.find('input[name=config_name]');
					let width = table.find('input[name=config_width]');
					let align = table.find('select[name=config_align]');
					grid_column_config_redraw(grid, name, hidden, header, width, align);
					grid_column_config_redraw(grid, name, hidden, header, width, align);
					save_grid_config(grid, settings.config_key);
					dropdown.dropdown('toggle');
					location.reload();
				});
				$('button.setting_reset', table).on('click', async function (e) {
					e.preventDefault();
					let question = await g_message('설정 초기화', '정말 이 화면의 개인설정을 초기화하시겠습니까?', 'warning', 'confirm');
					if (question.isConfirmed == true) {
						g_ajax('/member/ajax_delete_config', {
							config_key: settings.config_key,
						}).done(function (response) {
							if (response.code == 1) {
								g_toast('설정이 초기화되었습니다.');
								dropdown.dropdown('toggle');
								grid.setColumns(settings.defaultColumns);
								window.location.reload();
							}
						});
					}
				});
				table.sortable({
					items: 'tr.sortable',
					update: function () {
						let hidden = table.find('input[name=config_hidden]');
						let header = table.find('input[name=config_header]');
						let name = table.find('input[name=config_name]');
						let width = table.find('input[name=config_width]');
						let align = table.find('select[name=config_align]');
						grid_column_config_redraw(grid, name, hidden, header, width, align);
						grid_column_config_redraw(grid, name, hidden, header, width, align);
					},
				});
				dropdown
					.addClass('scroll-y')
					.addClass('grid_setting')
					.on('hide.bs.dropdown', function () {
						//dropdown.html('');
					});
				table.appendTo(form);
				form.appendTo(dropdown);
				init_scrollbar('.grid_setting', false);
			}
		});
	}
	return grid;
}

function g_append_pagenation_side_grid($grid_parent, text, font_size = 16, delay = 50, side = 'left') {
	setTimeout(function () {
		var $span = $('.tui-grid-pagination', $grid_parent).find(`span.${side}`);
		if (0 < $span.length) {
			$span.text(text);
		} else {
			$('.tui-grid-pagination', $grid_parent).append(
				`<span class="grid_bottom ${side}" style="font-size: ${font_size}px; float: ${side}; margin-top: 5px;">${text}</span>`
			);
		}
	}, delay);
}

/**
 * 요소(this)안에 데이터를 세팅한다.
 *
 * @return  {[type]}  [return description]
 */
$.fn.setupValues = function (data = {}, excludes = [], isTrigger) {
	excludes = undefined != excludes ? excludes : []; // 값세팅에 제외할 요소
	isTrigger = undefined != isTrigger ? isTrigger : false;

	$(this).each((index, ele) => {
		let tag = $(ele).prop('nodeName'); // 요소이 nodeName은 태그이름 (INPUT, DIV 등)
		let key = $(ele).attr('name');
		let type = $(ele).attr('type');
		let className = $(ele).attr('class') != undefined ? $(ele).attr('class') : [];

		let value = $(ele).val();
		for (let ele1 of excludes) {
			// 제외할 요소가 나오면 다음 턴으로
			if (ele1 == ele) {
				return false;
			}
		}

		let newValue = '';
		if (key && !Object.keys(data).includes(key)) {
			if ($(ele).is('[data-default_value]')) {
				newValue = $(ele).data('default_value');
			} else {
				return;
			}
		}
		// 데이터와 맞는 key요소가 존재할 경우
		if (key && Object.keys(data).includes(key)) {
			let changed = false;
			if ($(ele).hasClass('selectize')) {
				if ('undefined' !== typeof $(ele).attr('multiple') && false !== $(ele).attr('multiple')) {
					var options = [];
					var items = [];
					if (data[key]) {
						var arr = data[key].split('|');
						arr.forEach((value) => {
							options.push({
								value: value,
								text: value,
							});
							items.push(value);
						});
					}
					ele.selectize.addOption(options);
					ele.selectize.setValue(items);
				} else {
					ele.selectize.setValue(data[key]);
				}
			} else if ($(ele).hasClass('select2')) {
				$(ele).val(data[key]).trigger('change.select2');
			} else if ('INPUT' == tag && ('radio' == type || 'checkbox' == type) && value == data[key]) {
				if ('radio' == type && $(ele).closest('label.radio_btn').length) {
					$(ele).closest('label.radio_btn').addClass('active');
				}
				$(ele).prop('checked', true);
			} else {
				let oldValue = $(ele).val();
				if (oldValue != data[key]) {
					changed = true;
					if ('INPUT' == tag && $(ele).hasClass('datepicker') && '0000-00-00' == data[key]) {
						data[key] = '';
					}
					if ($(ele)[0].type != 'radio' && $(ele)[0].type != 'checkbox') {
						if ('INPUT' == tag && $.isNumeric(data[key]) && (className.includes('to_number') || className.includes('comma'))) {
							data[key] = comma(data[key]);
						}
						// if ($(ele).hasClass("datepicker")) $(ele).attr('data-sticky_date', data[key]);
						// $(ele).val(data[key]);
						if ($(ele).hasClass('datepicker')) {
							$(ele).attr('data-sticky_date', data[key]);
							$(ele).datepicker('setDate', data[key]);
						} else {
							$(ele).val(data[key]);
						}
					}
				}
			}
			if (isTrigger && changed) {
				$(ele).trigger('change');
			}
		}
	});
};

/**
 * 경고창을 띄운다. (제목, 텍스트, 아이콘, 경고창타입, 추가설정값)
 * @param {string} title 제목
 * @param {string} html 내용
 * @param {string} icon 아이콘 [warning, error, success, info, question] 기본값: info
 * @param {string} type 경고창 종류 [alert, confirm, prompt] 기본값: alert
 * @param {object} options 오버라이딩 할 옵션값
 */
function g_message(title = '', html = '', icon = 'info', type = 'alert', options = {}) {
	// NOTE 아래처럼 단순히 swal.fire 형태로 문구를 띄우는 것도 가능
	// g_message("결재문서", `검토자의 결재가 미결이 아닌 경우 결재자가 있어야 합니다.`, "warning");

	let settings = {
		title: title,
		html: html,
		icon: icon,
		confirmButtonText: '확인',
		cancelButtonText: '취소',
		denyButtonText: '아니오',
		returnFocus: false,
		showClass: {
			popup: 'swal2',
			backdrop: 'swal2-backdrop-show',
			icon: 'swal2-icon-show',
		},
		hideClass: {
			popup: 'swal2-hide',
			backdrop: 'swal2-backdrop-hide',
			icon: 'swal2-icon-hide',
		},
	};
	if (type == 'confirm') {
		settings = $.extend(settings, {
			showCancelButton: true,
			confirmButtonText: '예',
		});
	} else if (type == 'prompt') {
		settings = $.extend(settings, {
			showCancelButton: true,
			input: 'text',
		});
	} else if (type == 'toast') {
		settings = $.extend(settings, {
			toast: true,
			position: 'top-end',
			showConfirmButton: false,
			timer: 5000,
		});
	} else if (type == 'loading') {
	} else {
		settings = $.extend(settings, {
			timer: 7000,
			timerProgressBar: true,
		});
	}
	settings = $.extend(settings, options);
	return new Promise((resolve, reject) => {
		setTimeout(function () {
			// 포커스 문제때문에 타임아웃 0 추가가
			Swal.fire(settings)
				.then((result) => {
					resolve(result);
				})
				.catch((e) => {
					reject(e);
				});
		}, 0);
	});
}

// 디바운싱 이벤트
const debounce = (fn, wait = 250) => {
	let timer;
	return function (...args) {
		setTimeout(timer);
		timer = setTimeout(() => fn.apply(this, args), wait);
	};
};

// 날짜 전후 비교
function isValidateDate(start = '', end = '') {
	let result = {
		flag: true,
		msg: ''
	};
	if (!start && !end) {
		result.flag = true;
		return result;
	}
	if ((start && !end) || (!start && end)) {
		result.flag = false;
		result.msg = '기간 설정을 해주세요.';
		return result;
	}
	if (start > end) {
		result.flag = false;
		result.msg = '시작일자(일시)가 종료일자(일시)보다 최근일 수 없습니다.';
		return result;
	}
	return result;
}
