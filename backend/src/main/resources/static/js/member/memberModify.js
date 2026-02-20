$(function () {
	console.log('++ member/memberModify.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	urlSearch = new URLSearchParams(location.search); // 쿼리스트링 가져오기 (get으로 파라미터값을 가져올 수 있다.)
	const id = urlSearch.get('id');
	const menuPath = `직원관리 - 직원${id == null ? '등록' : '수정'}`;
	$('.topbar-inner .customBreadcrumb').text(menuPath);

	let isUseMiddleCodeData = [];
	let previewUrl = null; // 미리보기 이미지 객체
	let memberCodeAuthData = [];
	const $form = $('form.memberModifyForm', $modal);

	// 직원 등록/수정 페이지 렌더링 이후 초기화
	$modal.init_modal = async (param) => {
		$modal.param = param;

		// 직급관리, 부서관리 정보 세팅
		await $modal.setBasicOptions();

		// 수정인 경우, 직원정보를 세팅한다.
		if (id != null && id > 0) {
			$('input[name=loginId]', $modal).prop('readonly', true); // 로그인아이디 수정 불가

			// NOTE 수정인 경우, 아이디 항목 readonly 처리
			try {
				const resGetInfo = await gAjax(
					`/api/member/getMemberInfo?id=${id}`,
					{},
					{
						type: 'GET',
					}
				);
				if (resGetInfo?.code > 0) {
					// 데이터 세팅
					if (resGetInfo.data != undefined) {
						const memberInfo = resGetInfo.data.basicMemberInfo;
						$form.find('input[name], textarea[name], select[name]').setupValues(memberInfo);
						const imgFilePath = resGetInfo.data.memberImgPath;
						if (imgFilePath) {
							$modal.find('.memberImgEle').attr('src', imgFilePath).css('display', 'block');
						}
						const memberCodeAuth = resGetInfo.data.itemAuthData ?? [];
						memberCodeAuthData = memberCodeAuth;
					}
				}
			} catch (xhr) {
				console.error(xhr);
				customAjaxHandler(xhr);
			} finally {
			}
		}

		const authColumn = (header, name, headerEl, width) => ({
			header,
			name,
			width,
			align: 'center',
			renderer: { type: AuthCheckboxRenderer }, // 이게 핵심
			customHeader: headerEl, // 헤더 체크박스(텍스트+체크박스 DOM)
			sortable: false, // 권한컬럼은 보통 정렬 불필요(원하면 제거)
		});

		// 비트 상수
		$modal.AUTH_BIT = {
			WORKER: 1, // 실무자
			TECH_SUB: 2, // 기술책임자(부)
			TECH_MAIN: 4, // 기술책임자(정)
		};

		// authBitmask -> boolean 3개로 풀기
		$modal.applyAuthMaskToRow = (row) => {
			const mask = Number(row.authBitmask ?? 0);
			row.isWorker = (mask & $modal.AUTH_BIT.WORKER) !== 0;
			row.isTechSub = (mask & $modal.AUTH_BIT.TECH_SUB) !== 0;
			row.isTechMain = (mask & $modal.AUTH_BIT.TECH_MAIN) !== 0;
			return row;
		};

		// boolean 3개 -> authBitmask 합치기
		$modal.buildAuthMask = (row) => {
			let mask = 0;
			if (row.isWorker === true) mask |= $modal.AUTH_BIT.WORKER;
			if (row.isTechSub === true) mask |= $modal.AUTH_BIT.TECH_SUB;
			if (row.isTechMain === true) mask |= $modal.AUTH_BIT.TECH_MAIN;
			return mask;
		};

		// 헤더(텍스트 + 체크박스) DOM 생성
		$modal.createHeaderCheckbox = (title, columnName) => {
			const wrap = document.createElement('div');
			wrap.style.display = 'flex';
			wrap.style.alignItems = 'center';
			wrap.style.justifyContent = 'center';
			wrap.style.gap = '6px';

			const text = document.createElement('span');
			text.textContent = title;

			const checkBoxInput = document.createElement('input');
			checkBoxInput.type = 'checkbox';
			checkBoxInput.className = 'hdr-auth-checkbox';
			checkBoxInput.dataset.col = columnName;

			wrap.appendChild(text);
			wrap.appendChild(checkBoxInput);
			return wrap;
		};

		// 헤더 체크박스 상태(전체/부분/없음) 동기화
		$modal.syncHeaderCheckboxState = (grid, columnName, headerEl) => {
			const cb = headerEl.querySelector('input.hdr-auth-checkbox');
			if (!cb) return;

			const values = grid.getColumnValues(columnName) || [];
			const total = values.length;
			const checkedCount = values.reduce((acc, v) => acc + (v ? 1 : 0), 0);

			if (total === 0) {
				cb.checked = false;
				cb.indeterminate = false;
				return;
			}

			// 전부 체크일 때만 헤더 체크 ON
			const allChecked = checkedCount === total;

			cb.checked = allChecked;
			cb.indeterminate = false; // 핵심: 부분 상태(-)를 절대 쓰지 않음
		};

		// 헤더 엘리먼트 준비
		$modal.headerWorker = $modal.createHeaderCheckbox('실무자', 'isWorker');
		$modal.headerTechSub = $modal.createHeaderCheckbox('기술책임자(부)', 'isTechSub');
		$modal.headerTechMain = $modal.createHeaderCheckbox('기술책임자(정)', 'isTechMain');

		// Grid 생성
		$modal.initGrid = (isUseMiddleCodeData = []) => {
			$modal.itemAuthGrid = gGrid('.itemAuthGrid', {
				columns: [
					{
						header: '중분류코드',
						name: 'middleItemCode',
						width: 150,
						align: 'center',
					},
					{
						header: '중분류명',
						name: 'middleItemCodeName',
						align: 'left',
					},
					authColumn('실무자', 'isWorker', $modal.headerWorker, 140),
					authColumn('기술책임자(부)', 'isTechSub', $modal.headerTechSub, 160),
					authColumn('기술책임자(정)', 'isTechMain', $modal.headerTechMain, 160),
					{
						header: 'authBitmask',
						name: 'authBitmask',
						hidden: true,
					},
					{
						header: 'middleItemCodeId',
						name: 'middleItemCodeId',
						hidden: true,
					},
				],
				rowHeaders: [],
				bodyHeight: 500,
				minBodyHeight: 500,
				scrollY: true,    // 세로 스크롤 활성화
				pageOptions: {},  // gGrid 기본값(perPage:20) 덮어쓰기 → 페이지네이션 비활성화
				data: [],
			});

			// 중분류 그리드 세팅
			if (isUseMiddleCodeData.length > 0) {
				// 체크박스 컬럼은 반드시 boolean 초기값을 넣어준다.
				const rows = (isUseMiddleCodeData || []).map((midData) => ({
					middleItemCodeId: midData.id, // FK로 쓸 id
					middleItemCode: midData.codeNum, // 화면용 코드(101,102...)
					middleItemCodeName: midData.codeName, // 화면용 명칭
					authBitmask: 0, // 초기 마스크 0
					isWorker: false, // 체크박스 초기값
					isTechSub: false,
					isTechMain: false,
				}));

				// 그리드에 세팅
				$modal.itemAuthGrid.resetData(rows);
			}
		};

		// 그리드를 렌더링하고 중분류 데이터셋을 표시한다.
		$modal.initGrid(isUseMiddleCodeData);
		// 직원수정인 경우, 해당 직원의 분야별 권한을 표시한다.
		if (id > 0 && memberCodeAuthData.length > 0) {
			// 1) memberCodeAuthData -> Map(middleItemCodeId => authBitmask)
			const authMap = new Map(memberCodeAuthData.map((a) => [Number(a.middleItemCodeId), Number(a.authBitmask ?? 0)]));

			// 2) 그리드에 이미 세팅된 기본 rows 가져오기
			const baseRows = $modal.itemAuthGrid.getData();
			// getData()는 컬럼명 기준 객체 배열 반환(43개)

			// 3) rows에 비트마스크를 적용해서 boolean 컬럼까지 채운 새 rows 만들기
			const mergedRows = baseRows.map((r) => {
				const mask = authMap.get(Number(r.middleItemCodeId)) ?? 0;

				return {
					...r,
					authBitmask: mask,
					isWorker: (mask & 1) !== 0,
					isTechSub: (mask & 2) !== 0,
					isTechMain: (mask & 4) !== 0,
				};
			});

			// 4) 한 번에 반영 (이게 제일 안정적)
			$modal.itemAuthGrid.resetData(mergedRows);

			// 5) 헤더 체크박스 상태(전체/부분)도 다시 맞추고 싶으면
			if (typeof $modal.syncAllAuthHeaders === 'function') {
				$modal.syncAllAuthHeaders();
			}
		}

		// 헤더 체크박스 클릭 시 해당 컬럼 전체 토글
		$modal.bindHeaderCheckbox = (grid, columnName, headerEl) => {
			const cb = headerEl.querySelector('input.hdr-auth-checkbox');
			if (!cb) return;

			cb.addEventListener('mousedown', (e) => e.stopPropagation());

			cb.addEventListener('click', (e) => {
				e.stopPropagation();
				grid.finishEditing();

				const checked = cb.checked;

				// (핵심) setColumnValues 대신 row별 setValue로 확실하게 데이터 반영
				const rowCount = grid.getRowCount();
				for (let i = 0; i < rowCount; i++) {
					const row = grid.getRowAt(i);
					const rowKey = row.rowKey;

					// boolean 값 반영
					grid.setValue(rowKey, columnName, checked, false);

					// 즉시 bitmask도 같이 반영(일괄 변경은 여기서 강제 동기화)
					const newMask = $modal.buildAuthMask(grid.getRow(rowKey));
					grid.setValue(rowKey, 'authBitmask', newMask, false);
				}

				// 헤더 상태 갱신
				$modal.syncHeaderCheckboxState(grid, columnName, headerEl);
			});
		};

		// 헤더 체크박스 바인딩
		$modal.bindHeaderCheckbox($modal.itemAuthGrid, 'isWorker', $modal.headerWorker);
		$modal.bindHeaderCheckbox($modal.itemAuthGrid, 'isTechSub', $modal.headerTechSub);
		$modal.bindHeaderCheckbox($modal.itemAuthGrid, 'isTechMain', $modal.headerTechMain);

		// 셀 변경 시 authBitmask 동기화 + 헤더 상태 동기화
		$modal.itemAuthGrid.on('afterChange', (ev) => {
			const changes = ev?.changes;
			if (!Array.isArray(changes) || changes.length === 0) return;

			const authCols = new Set(['isWorker', 'isTechSub', 'isTechMain']);
			const touchedRowKeys = new Set();
			const touchedCols = new Set();

			changes.forEach((c) => {
				if (authCols.has(c.columnName)) {
					touchedRowKeys.add(c.rowKey);
					touchedCols.add(c.columnName); // 어떤 컬럼이 바뀌었는지 기록
				}
			});

			// 1) 권한 컬럼이 바뀐 행만 bitmask 갱신
			touchedRowKeys.forEach((rowKey) => {
				const row = $modal.itemAuthGrid.getRow(rowKey);
				const newMask = $modal.buildAuthMask(row);
				$modal.itemAuthGrid.setValue(rowKey, 'authBitmask', newMask, false);
			});

			// 2) (추가) 헤더 체크박스 상태 자동 갱신
			// - 하나라도 해제되면 cb.checked=false
			// - 모두 체크되면 cb.checked=true
			// - 일부만 체크면 indeterminate=true
			touchedCols.forEach((col) => {
				if (col === 'isWorker') $modal.syncHeaderCheckboxState($modal.itemAuthGrid, col, $modal.headerWorker);
				if (col === 'isTechSub') $modal.syncHeaderCheckboxState($modal.itemAuthGrid, col, $modal.headerTechSub);
				if (col === 'isTechMain') $modal.syncHeaderCheckboxState($modal.itemAuthGrid, col, $modal.headerTechMain);
			});
		});

		// 전체 헤더 상태 한 번에 동기화
		$modal.syncAllAuthHeaders = () => {
			$modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isWorker', $modal.headerWorker);
			$modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isTechSub', $modal.headerTechSub);
			$modal.syncHeaderCheckboxState($modal.itemAuthGrid, 'isTechMain', $modal.headerTechMain);
		};
	}; // End init_modal

	// 부서관리, 직급관리 정보를 가져와서 세팅한다.
	$modal.setBasicOptions = async () => {
		const resGetOptions = await gAjax(
			'/api/basic/getBasicOptions',
			{},
			{
				type: 'GET',
			}
		);

		if (resGetOptions?.code > 0) {
			const resData = resGetOptions.data;
			// 부서관리 옵션을 세팅한다.
			if (resData.departmentData != undefined && resData.departmentData.length > 0) {
				const $departmentSelect = $('.departmentSelect', $modal);
				$.each(resData.departmentData, function (index, data) {
					const option = new Option(data.name, data.id);
					$departmentSelect.append(option);
				});
			}
			// 직급관리 옵션을 세팅한다.
			if (resData.memberLevelData != undefined && resData.memberLevelData.length > 0) {
				const $memberLevelSelect = $('.memberLevelSelect', $modal);
				$.each(resData.memberLevelData, function (index, data) {
					const option = new Option(data.name, data.id);
					$memberLevelSelect.append(option);
				});
			}
			// 중분류코드를 세팅한다.
			if (resData.isUseMiddleCodeData != undefined && resData.isUseMiddleCodeData.length > 0) {
				isUseMiddleCodeData = resData.isUseMiddleCodeData;
			}
		}
	};

	// 페이지 내 이벤트
	$modal
		// 저장
		.on('click', '.memberSave', async function (e) {
			const $btn = $(this);
			const formData = new FormData($form[0]);
			$modal.itemAuthGrid.blur();

			// 입력값 검증
			let isFormValid = true;
			try {
				$btn.prop('disabled', true);
				// formData에 모두 담아서 POST로 전송 (null의 경우 문자열 'null'로 넘어가는 것 주의)
				const pwd = formData.get('pwd');
				const pwdConfirm = formData.get('pwdConfirm');

				// 수정
				if (id > 0) {
					// 수정일 경우, 비밀번호는 값이 있을 때만 체크한다.
					if (pwdConfirm) {
						// 비밀번호
						if (pwd !== pwdConfirm) {
							isFormValid = false;
							throw new Error('비밀번호 확인값과 일치하지 않습니다');
						}
						// 비밀번호 정규식 체크
						if (!checkPwd(pwdConfirm)) {
							isFormValid = false;
							throw new Error('비밀번호는 소문자, 대문자, 숫자, 특수문자(!@#$%^)들로 구성된 8~20자리여야 합니다.');
						}
					}
					formData.append('id', id);
				}
				// 등록
				else {
					// 아이디
					const loginId = formData.get('loginId');
					if (!checkLoginId(loginId)) {
						isFormValid = false;
						throw new Error('아이디는 영어소문자로 시작해서 숫자를 포함하여 4~20자리로 구성되어야 합니다.');
					}

					// 비밀번호
					if (pwd !== pwdConfirm) {
						isFormValid = false;
						throw new Error('비밀번호 확인값과 일치하지 않습니다');
					}
					// 비밀번호 정규식 체크
					if (!checkPwd(pwd)) {
						isFormValid = false;
						throw new Error('비밀번호는 소문자, 대문자, 숫자, 특수문자(!@#$%^)들로 구성된 8~20자리여야 합니다.');
					}
				}

				// 이름 확인
				const name = formData.get('name');
				if (!checkInput(name)) {
					isFormValid = false;
					throw new Error('이름을 입력해주세요.');
				}

				// 이메일 정규식 체크 (등록/수정 공통 체크)
				const email = formData.get('email');
				if (!checkEmailReg(email)) {
					isFormValid = false;
					throw new Error('유효하지 않은 이메일 형식입니다.');
				}
			} catch (err) {
				gToast(err ?? '입력항목에 오류가 있습니다.', 'warning');
				$btn.prop('disabled', false);
				isFormValid = false;
			} finally {
				if (!isFormValid) {
					return false;
				}
			}

			// 입사일, 퇴사일, 생일은 빈 값일 경우 서버에서 매핑하는 과정에서 오류가 생길 수 있음. 체크
			const birth = formData.get('birth'); // 생일
			if (!birth) {
				formData.delete('birth');
			}
			const joinDate = formData.get('joinDate'); // 입사일자
			if (!joinDate) {
				formData.delete('joinDate');
			}
			const leaveDate = formData.get('leaveDate'); // 퇴사일자
			if (!leaveDate) {
				formData.delete('leaveDate');
			}

			// 그리드 데이터 담기
			const itemAuthData = $modal.itemAuthGrid.getData();
			// FormData에 그리드 데이터를 담아서 서버에 전송하기 위해선 아래 방식 또는 Blob을 통해 @RequestPart를 사용해야 함.
			itemAuthData.forEach((row, i) => {
				formData.append(`itemAuthData[${i}].middleItemCodeId`, row.middleItemCodeId ?? '');
				formData.append(`itemAuthData[${i}].authBitmask`, row.authBitmask ?? 0);
				// memberId는 서버에서 주입할 거면 아예 보내지 않는 편이 안전
				// formData.append(`itemAuthData[${i}].memberId`, memberId); // 필요 시만
			});
			// formData.append('itemAuthData', JSON.stringify(itemAuthData));

			// 입력값 확인
			for (const [key, value] of formData.entries()) {
				console.log('key: ' + key + ' value: ' + value);
			}

			// 저장 진행
			const saveTypeKr = id != null && id > 0 ? '수정' : '등록';
			const saveConfirm = await gMessage(`직원정보 ${saveTypeKr}`, '저장하시겠습니까?', 'question', 'confirm');
			if (saveConfirm.isConfirmed === true) {
				gLoadingMessage();
				try {
					const feOptions = {
						method: 'POST',
						body: formData,
					};
					const resSave = await fetch('/api/member/memberSave', feOptions);
					if (resSave.ok) {
						const resData = await resSave.json();
						if (resData?.code > 0) {
							await gMessage(`직원정보 ${saveTypeKr}`, '저장되었습니다.', 'success', 'alert');
							if (resData.data != undefined && resData.data > 0) {
								const savedId = Number(resData.data);
								location.href = `/member/memberModify?id=${savedId}`;
							} else {
								location.href = `/member/memberManage`;
							}
						} else {
							await gMessage(`직원정보 ${saveTypeKr}`, resData.msg ?? '저장하는 데 실패했습니다', 'warning', 'alert');
						}
					} else {
					}
				} catch (xhr) {
					customAjaxHandler(xhr);
				} finally {
					Swal.close();
					$btn.prop('disabled', false);
				}
			} else {
				$btn.prop('disabled', false);
			}
		})
		// 이미지 업로드 변경 체크
		.on('change', 'input[name=memberImage]', function (e) {
			const file = e.target.files?.[0];
			if (!file) {
				return false;
			}

			if (!file.type.startsWith('image/')) {
				gToast('이미지 파일만 업로드 가능합니다.', 'warning');
				$(this).val('');
				return false;
			}
			// 미리보기 이미지 객체가 존재하는 경우, 삭제시킨다.
			if (previewUrl) {
				URL.revokeObjectURL(previewUrl);
			}
			previewUrl = URL.createObjectURL(file);
			$modal.find('.memberImgEle').attr('src', previewUrl).css('display', 'block');
		})
		// 이미지 삭제 클릭
		.on('click', '.deleteUserImg', async function (e) {
			// 이미지 파일이 존재하는 경우, db에서 삭제를 시킨다.
			const imgFileId = $('input[name=imgFileId]', $modal).val();
			if (imgFileId > 0) {
				const deleteCheck = await gMessage('이미지 삭제', '기존 업로드 이미지를 삭제하시겠습니까?', 'question', 'confirm');
				if (deleteCheck.isConfirmed === true) {
					gLoadingMessage();
					try {
						const resDelete = await gAjax('/api/file/fileDelete/' + imgFileId);
						Swal.close(); // 통신이 끝나면 로딩창을 닫는다.
						if (resDelete?.code > 0) {
							await gMessage(`이미지 삭제`, `삭제되었습니다.`, 'success', 'alert');
							$('input[name=imgFileId]', $modal).val('');
						} else {
							await gMessage(`이미지 삭제`, `삭제처리에 실패했습니다.`, 'warning', 'alert');
						}
					} catch (xhr) {
						customAjaxHandler(xhr);
					} finally {
						Swal.close();
					}
				}
			} else {
				return false;
			}

			// 미리보기 객체가 있다면, 지우고 기본 이미지 경로로 교체한다.
			if (previewUrl) {
				URL.revokeObjectURL(previewUrl);
			}
			// input file 초기화
			$('input[name=memberImage]', $modal).val('');
			$modal.find('.memberImgEle').attr('src', '/images/basic_user.png').css('display', 'block');
		})
		// 리스트 돌아가기
		.on('click', '.goBack', async function (e) {
			const confirm = await gMessage('직원관리 이동', '리스트로 이동하시겠습니까?', 'question', 'confirm');
			if (confirm.isConfirmed === true) {
				location.href = '/member/memberManage';
			} else {
				return false;
			}
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
		// 모달이 아닌 일반 페이지인 경우엔 아래 initPage가 동작한다.
		if (!$modal_root.length) {
			initPage($modal);
		}
	}
});
