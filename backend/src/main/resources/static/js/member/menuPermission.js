$(function () {
	console.log('++ member/menuPermission.js');

	const $candidates = $('.modal-view:not(.modal-view-applied)');
	let $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	let memberId = null;

	$modal.init_modal = async (param) => {
		$modal.param = param;
		memberId = param?.memberId;

		$modal.grid = gGrid('.menuPermissionGrid', {
			columns: [
				{
					header: '메뉴명',
					name: 'menuAlias',
					formatter: function ({ row, value }) {
						const depth = row.depth ?? 1;
						const prefix = depth >= 2 ? '&nbsp;&nbsp;&nbsp;&nbsp;└─&nbsp;' : '';
						return prefix + value;
					},
				},
			],
			rowHeaders: ['checkbox'],
			pageOptions: false,
			minBodyHeight: 420,
			bodyHeight: 420,
		});

		try {
			const res = await fetch(`/api/member/${memberId}/menuPermissions`);
			if (!res.ok) throw res;
			const resData = await res.json();
			const { isAdminMember, items } = resData.data;

			$modal.grid.resetData(items);

			// rowKey 기반 부모-자식 인덱스 구성
			const parentToChildren = {};
			const childToParent = {};
			items.forEach((item, rowKey) => {
				if (item.parentId != null) {
					const parentRowKey = items.findIndex((i) => i.id === item.parentId);
					if (parentRowKey >= 0) {
						if (!parentToChildren[parentRowKey]) parentToChildren[parentRowKey] = [];
						parentToChildren[parentRowKey].push(rowKey);
						childToParent[rowKey] = parentRowKey;
					}
				}
			});

			// hasPermission = true 인 행 체크
			items.forEach((item, rowKey) => {
				if (item.hasPermission) {
					$modal.grid.check(rowKey);
				}
			});

			// 부모-자식 체크박스 연동 (재귀 방지 가드 포함)
			let isSyncing = false;

			$modal.grid.on('check', function (e) {
				if (isSyncing) return;
				isSyncing = true;
				try {
					const rowKey = e.rowKey;
					// 부모 체크 → 자식 전체 체크
					if (parentToChildren[rowKey]) {
						parentToChildren[rowKey].forEach((childRowKey) => $modal.grid.check(childRowKey));
					}
					// 자식 체크 → 부모 체크
					if (childToParent[rowKey] !== undefined) {
						$modal.grid.check(childToParent[rowKey]);
					}
				} finally {
					isSyncing = false;
				}
			});

			$modal.grid.on('uncheck', function (e) {
				if (isSyncing) return;
				isSyncing = true;
				try {
					const rowKey = e.rowKey;
					// 부모 해제 → 자식 전체 해제
					if (parentToChildren[rowKey]) {
						parentToChildren[rowKey].forEach((childRowKey) => $modal.grid.uncheck(childRowKey));
					}
					// 자식 해제 → 형제 중 체크된 항목 없으면 부모도 해제
					if (childToParent[rowKey] !== undefined) {
						const parentRowKey = childToParent[rowKey];
						const checkedRowKeys = new Set($modal.grid.getCheckedRowKeys());
						const anyChecked = (parentToChildren[parentRowKey] || []).some(
							(k) => k !== rowKey && checkedRowKeys.has(k),
						);
						if (!anyChecked) {
							$modal.grid.uncheck(parentRowKey);
						}
					}
				} finally {
					isSyncing = false;
				}
			});

			// admin 계정이면 저장 버튼 비활성화
			if (isAdminMember) {
				$modal_root.find('.modal-btn-confirm').prop('disabled', true);
			}
		} catch (err) {
			gApiErrorHandler(err);
		}
	};

	// 저장 버튼 클릭 시 confirm_modal 호출됨
	$modal.confirm_modal = async () => {
		const checkedRows = $modal.grid.getCheckedRows();
		const menuIds = checkedRows.map((row) => row.id);

		const confirmResult = await gMessage('메뉴 권한 저장', '메뉴 권한을 저장하시겠습니까?', 'question', 'confirm');
		if (!confirmResult.isConfirmed) return false;

		gLoadingMessage();
		try {
			const res = await fetch(`/api/member/${memberId}/menuPermissions`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ menuIds }),
			});
			Swal.close();
			if (!res.ok) throw res;
			const resData = await res.json();
			if (resData?.code > 0) {
				await gMessage(
					'메뉴 권한 저장',
					'저장되었습니다.<br>변경된 권한은 해당 직원이 로그아웃 후<br>재로그인해야 반영됩니다.',
					'success',
					'alert',
				);
				return true;
			} else {
				await gMessage('메뉴 권한 저장', resData?.msg ?? '저장에 실패했습니다.', 'warning', 'alert');
				return false;
			}
		} catch (err) {
			Swal.close();
			gApiErrorHandler(err);
			return false;
		}
	};

	$modal.data('modal-data', $modal);
	$modal.addClass('modal-view-applied');
	if ($modal.hasClass('modal-body')) {
		//모달 팝업창인 경우 바로 init_modal() 호출
		setTimeout(() => {
			const p = $modal.data('param') || {};
			$modal.init_modal(p);
			if (typeof $modal.grid == 'object') {
				$modal.grid.refreshLayout();
			}
		}, 200);
	}

	if (typeof window.modal_deferred == 'object') {
		window.modal_deferred.resolve('script end');
	} else {
		if (!$modal_root.length) {
			initPage($modal);
		}
	}
});
