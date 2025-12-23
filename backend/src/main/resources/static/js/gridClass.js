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

// 소분류 select box 커스텀
class small_code_selectbox_renderer {
	constructor(props) {
		const { grid, rowKey, value } = props;

		// 내부 store 접근 대신 공식 API 권장
		const rowData = grid.getRow(rowKey) || {};

		const allItems = props.columnInfo.editor.options.listItems; // tmpSmallCode (flat array)
		const middleId = rowData.middleItemCodeId; // ✅ 컬럼명과 동일하게

		// ✅ middleId에 맞게 필터링
		const filtered = allItems.filter((it) => String(it.middleCodeId) === String(middleId));

		const el = document.createElement('select');
		el.style.width = '100%';
		el.style.height = '80%';
		el.style.border = '1px solid #ddd';

		// ✅ 그리드가 mousedown/click 받으면 편집 종료해버리므로 차단
		this._stop = (e) => e.stopPropagation();
		el.addEventListener('mousedown', this._stop);
		el.addEventListener('click', this._stop);

		// 기본 옵션
		el.append(new Option('선택', ''));

		for (const item of filtered) {
			const opt = new Option(item.text, item.value);
			if (String(value) === String(item.value)) opt.selected = true; // ✅ value 기준
			el.append(opt);
		}

		this.el = el;
	}

	getElement() {
		return this.el;
	}
	getValue() {
		return this.el.value;
	}

	mounted() {
		this.el.focus(); // ✅ 포커스 주기
	}

	beforeDestroy() {
		// ✅ 리스너 정리
		this.el.removeEventListener('mousedown', this._stop);
		this.el.removeEventListener('click', this._stop);
	}
}

// 중분류 select box 커스텀
class middle_code_selectbox_renderer {
	constructor(props) {
		const { value } = props;
		const items = props.columnInfo.editor.options.listItems; // tmpMiddleCode (array)

		const el = document.createElement('select');
		el.style.width = '100%';
		el.style.height = '80%';
		el.style.border = '1px solid #ddd';

		this._stop = (e) => e.stopPropagation();
		el.addEventListener('mousedown', this._stop);
		el.addEventListener('click', this._stop);

		el.append(new Option('선택', ''));

		for (const item of items) {
			const opt = new Option(item.text, item.value);
			if (String(value) === String(item.value)) opt.selected = true; // ✅ value 기준
			el.append(opt);
		}

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
	}

	beforeDestroy() {
		this.el.removeEventListener('mousedown', this._stop);
		this.el.removeEventListener('click', this._stop);
	}
}
