// 그리드 셀에 연락처 입력 시, 하이픈 형태가 생기도록
console.log('++ js/gridClass.js');
class contact_num_editor {
	constructor(props) {
		const el = document.createElement('input');
		el.type = 'text';
		el.value = String(props.value ?? '');
		el.style.width = '100%';
		el.style.height = '100%';

		// keyup 시 전화번호 포맷 적용
		el.addEventListener('keyup', function () {
			let input = el.value;

			const tel = input.replace(/[^0-9]/g, '');

			if (/^02\d{7,8}$/.test(tel)) {
				input = tel.replace(/^(02)(\d{3,4})(\d{4})$/, '$1-$2-$3');
			} else {
				input = tel.replace(/^(\d{2,3})(\d{3,4})(\d{4})$/, `$1-$2-$3`);
			}

			el.value = input;
		});

		this.el = el;
	}

	getElement() {
		return this.el;
	}

	getValue() {
		return this.el.value;
	}

	mounted() {
		this.el.focus();
		this.el.select();
	}

	beforeDestroy() {}
}
