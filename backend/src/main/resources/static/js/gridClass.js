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

// editor에서 특정조건이 있는 경우엔 편집이 안 되도록 설정한다.
class readOnlyEditorByCondition {
	constructor(props) {
		const { grid, rowKey, value } = props;
		const rowData = grid.getRow(rowKey);

		const condition = props.columnInfo.editor.conditions;

		const el = document.createElement('input');
		el.type = 'text';
		el.value = String(props.value ?? '');
		el.style.width = '100%';
		el.style.height = '100%';
		el.style.textAlign = "center";

		
		Object.entries(condition).forEach(([key, value]) => {
			if (rowData[key] == value) {
				el.readOnly = true;
				el.disabled = true;
			}
		})

		this.el = el;
	}

	getElement() {
		return this.el;
	}

	getValue() {
		return this.el.value.trim();
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

// 금액만 입력가능
class number_format_editor {
  constructor(props) {
    const el = document.createElement('input');
    el.type = 'text';
    el.inputMode = 'numeric';      // 모바일 키패드 숫자 유도
    el.autocomplete = 'off';
    el.spellcheck = false;

    el.style.width = '100%';
    el.style.height = '100%';
    el.style.boxSizing = 'border-box';

    // 그리드 편집 종료(blur) 트리거 방지용(필요 시)
    const stop = (e) => e.stopPropagation();
    el.addEventListener('mousedown', stop);
    el.addEventListener('click', stop);
    this._stop = stop;

    // 초기값 세팅
    el.value = this._format(props.value);

    // - / + / e / . 등 차단 (type="number"에서 흔히 허용되는 것들)
    el.addEventListener('keydown', (e) => {
      const blocked = ['-', '+', 'e', 'E', '.', ',']; // 콤마는 우리가 자동 삽입하므로 직접 입력은 차단
      if (blocked.includes(e.key)) e.preventDefault();
    });

    // 붙여넣기 방어: 숫자 외 제거 후 포맷 적용
    el.addEventListener('paste', (e) => {
      e.preventDefault();
      const text = (e.clipboardData || window.clipboardData).getData('text') || '';
      const digits = text.replace(/[^\d]/g, '');
      el.value = this._format(digits);
    });

    // 입력될 때마다 숫자만 남기고 콤마 포맷
    el.addEventListener('input', () => {
      const digits = this._digits(el.value);
      el.value = this._format(digits);
      // 커서 위치를 정교하게 유지하려면 추가 로직이 필요하지만,
      // 대부분 “끝으로 이동”도 UX에 무리가 없습니다.
    });

    this.el = el;
  }

  _digits(v) {
    return String(v ?? '').replace(/[^\d]/g, ''); // 숫자만
  }

  _format(v) {
    const digits = this._digits(v);

    // 빈 값 허용 여부: "0 이상"이지만 사용자가 지우는 과정이 있으니 입력 중엔 빈값 허용
    if (digits === '') return '';

    // 선행 0 정리(예: 00012 -> 12), 단 "0" 자체는 유지
    const normalized = digits.replace(/^0+(?=\d)/, '');

    // 천 단위 콤마
    return normalized.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  }

  getElement() {
    return this.el;
  }

  getValue() {
    const digits = this._digits(this.el.value);

    // 빈 값 처리 정책:
    // - 금액 필드가 NOT NULL이면 0으로 저장하는 게 보통 더 안전
    if (digits === '') return 0;

    const n = Number(digits);
    return Number.isFinite(n) ? n : 0;
  }

  mounted() {
    this.el.focus();
    this.el.select();
  }

  beforeDestroy() {
    // (선택) 이벤트 정리
    this.el.removeEventListener('mousedown', this._stop);
    this.el.removeEventListener('click', this._stop);
  }
}