$(function () {
	console.log('++ dev/workModify.js');

	// ── $modal / $modal_root 설정 (표준 패턴) ──────────────────────────
	const $candidates    = $('.modal-view:not(.modal-view-applied)');
	let   $modal;
	const $bodyCandidate = $candidates.filter('.modal-body');
	if ($bodyCandidate.length) {
		$modal = $bodyCandidate.first();
	} else {
		$modal = $candidates.first();
	}
	let $modal_root = $modal.closest('.modal');

	// ── 모듈 레벨 상태 변수 ────────────────────────────────────────────
	let editor      = null;       // Toast UI Editor 인스턴스 (작성 패널)
	let viewer      = null;       // Toast UI Viewer 인스턴스 (상세 패널)
	let attachFiles = [];         // 작성 패널에서 선택된 첨부파일 목록
	let activeTab   = 'inquiry';  // 현재 활성 탭 ('notice' | 'inquiry')

	const MAX_FILES     = 10;
	const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

	// 유형/상태/중요도 한글 레이블
	const CATEGORY_LABEL = { BUG: '버그', ERROR: '오류', NEW_FEATURE: '기능 요청', INQUIRY: '문의' };
	const STATUS_LABEL   = { READY: '접수', IN_PROGRESS: '처리중', ON_HOLD: '보류', DONE: '완료', CANCELLED: '취소' };
	const STATUS_CLASS   = {
		READY: 'badge-secondary', IN_PROGRESS: 'badge-primary',
		ON_HOLD: 'badge-warning', DONE: 'badge-success', CANCELLED: 'badge-dark',
	};
	const PRIORITY_LABEL = { NORMAL: '일반', EMERGENCY: '긴급' };
	const PRIORITY_CLASS = { NORMAL: 'badge-info', EMERGENCY: 'badge-danger' };

	// ── Toast UI Editor 동적 로드 (vendor 로컬 파일) ──────────────────
	// config.html 공통 블록에 포함하지 않고 이 모달 최초 오픈 시 1회만 로드
	function loadEditorLibrary() {
		return new Promise((resolve) => {
			if (window.toastui && window.toastui.Editor) { resolve(); return; }
			const link  = document.createElement('link');
			link.rel    = 'stylesheet';
			link.href   = '/vendor/tui-editor/toastui-editor.min.css';
			document.head.appendChild(link);
			const script  = document.createElement('script');
			script.src    = '/vendor/tui-editor/toastui-editor-all.min.js';
			script.onload = resolve;
			document.head.appendChild(script);
		});
	}

	// ── 탭 전환 ────────────────────────────────────────────────────────
	// 작성 패널 진행 중 탭 이동 시 dirty 체크 포함
	$modal.switchTab = async function (tabName) {
		if (activeTab === tabName) return;

		// 작성 패널이 열려 있고 내용이 있으면 확인
		const isWriteOpen = $('.panel-write', $modal).not('.d-none').length > 0;
		if (isWriteOpen) {
			const hasTitle   = $('input[name=title]', $modal).val().trim().length > 0;
			const hasContent = editor && editor.getMarkdown().trim().length > 0;
			if (hasTitle || hasContent) {
				const confirmed = await gMessage(
					'작성 중인 내용이 있습니다. 이동하시겠습니까?', '', 'warning', 'confirm'
				);
				if (!confirmed.isConfirmed) return;
			}
			// 문의 탭의 패널을 목록으로 되돌림
			$modal.showPanel('list');
		}

		activeTab = tabName;

		// 탭 버튼 active 상태 전환
		$('.work-tab-btn', $modal).removeClass('active');
		$(`.work-tab-btn[data-tab="${tabName}"]`, $modal).addClass('active');

		// 탭 컨텐츠 전환
		$('.work-tab-pane', $modal).addClass('d-none');
		$(`.work-tab-pane[data-pane="${tabName}"]`, $modal).removeClass('d-none');

		// 등록 버튼: 문의 탭의 작성 패널에서만 노출
		$modal_root.find('.modal-btn-confirm').hide();
	};

	// ── 문의 탭 내 패널 전환 ──────────────────────────────────────────
	// panelName: 'list' | 'write' | 'detail'
	$modal.showPanel = function (panelName) {
		$('.work-panel', $modal).addClass('d-none');
		$(`.panel-${panelName}`, $modal).removeClass('d-none');

		// 등록 버튼: 작성 패널에서만 노출
		const $confirmBtn = $modal_root.find('.modal-btn-confirm');
		if (panelName === 'write') {
			$confirmBtn.show();
		} else {
			$confirmBtn.hide();
		}
	};

	// ── 문의 목록 렌더링 (추후 API 연동) ─────────────────────────────
	$modal.loadWorkList = function () {
		$('.work-list-empty', $modal).show();
	};

	// ── 공지 목록 렌더링 (추후 API 연동) ─────────────────────────────
	$modal.loadNoticeList = function () {
		$('.notice-list-empty', $modal).show();
	};

	// ── 작성 패널 초기화 ───────────────────────────────────────────────
	$modal.initWritePanel = async function () {
		// 작성자 세팅 (부모 페이지 G_USER 전역변수 활용)
		if (typeof G_USER !== 'undefined') {
			$('.writerNameDisplay', $modal).val(G_USER.name || '');
		}

		// 폼 초기화
		$('form.workWriteForm', $modal)[0]?.reset();
		attachFiles = [];
		$('.attach-file-list', $modal).empty();

		// Toast UI Editor 초기화 (1회만 생성, 이후 재사용)
		if (!editor) {
			await loadEditorLibrary();
			editor = new toastui.Editor({
				el:              $modal.find('.workEditorEl')[0],
				height:          '280px',
				initialEditType: 'wysiwyg',
				previewStyle:    'vertical',
				placeholder:     '문의 내용을 입력하세요. 이미지는 붙여넣기 또는 드래그로 첨부할 수 있습니다.',
				toolbarItems: [
					['heading', 'bold', 'italic', 'strike'],
					['hr', 'quote'],
					['ul', 'ol'],
					['table', 'image', 'link'],
					['code', 'codeblock'],
				],
			});
		} else {
			editor.setMarkdown('');
		}
	};

	// ── 상세 패널 렌더링 ───────────────────────────────────────────────
	$modal.renderDetail = async function (workData) {
		$('.work-category-badge', $modal)
			.text(CATEGORY_LABEL[workData.category] ?? workData.category)
			.attr('class', 'badge badge-secondary work-category-badge');

		$('.work-priority-badge', $modal)
			.text(PRIORITY_LABEL[workData.priorityByAgent] ?? workData.priorityByAgent)
			.attr('class', `badge ${PRIORITY_CLASS[workData.priorityByAgent] ?? 'badge-info'} work-priority-badge`);

		$('.work-status-badge', $modal)
			.text(STATUS_LABEL[workData.workStatus] ?? workData.workStatus)
			.attr('class', `badge ${STATUS_CLASS[workData.workStatus] ?? 'badge-secondary'} work-status-badge`);

		$('.work-title-display',  $modal).text(workData.title ?? '');
		$('.work-date-display',   $modal).text(workData.createdAt ? workData.createdAt.substring(0, 10) : '');
		$('.work-writer-display', $modal).text(workData.createMemberName ?? '');

		// Toast UI Viewer (본문)
		if (!viewer) {
			await loadEditorLibrary();
			viewer = new toastui.Viewer({
				el:           $modal.find('.workViewerEl')[0],
				initialValue: workData.content ?? '',
			});
		} else {
			viewer.setMarkdown(workData.content ?? '');
		}

		// 첨부파일
		const attachList = workData.attachments ?? [];
		if (attachList.length > 0) {
			$('.work-attach-footer', $modal).removeClass('d-none');
			const $attachUl = $('.detail-attach-list', $modal).empty();
			attachList.forEach(f => {
				$attachUl.append(
					`<li><a href="#" class="text-primary" data-path="${f.storedPath}">
						<i class="bi bi-file-earmark"></i> ${$('<span>').text(f.originalName).html()}
					</a></li>`
				);
			});
		} else {
			$('.work-attach-footer', $modal).addClass('d-none');
		}

		$modal.renderComments(workData.comments ?? []);
	};

	// ── 댓글 목록 렌더링 ───────────────────────────────────────────────
	$modal.renderComments = function (comments) {
		const $wrap = $('.work-comments', $modal).empty();

		if (comments.length === 0) {
			$wrap.append(`<div class="text-center text-muted py-2" style="font-size:.85rem;">아직 댓글이 없습니다.</div>`);
			return;
		}

		comments.forEach(c => {
			const isDev   = c.authorType === 'DEV';
			const dateStr = c.createdAt ? c.createdAt.substring(0, 16).replace('T', ' ') : '';
			$wrap.append(`
				<div class="comment-item ${isDev ? 'comment-dev' : 'comment-agent'} mb-2">
					<div class="comment-meta d-flex align-items-center" style="gap:6px;font-size:.8rem;">
						<span class="badge ${isDev ? 'badge-dark' : 'badge-secondary'}">${isDev ? '개발팀' : '업체'}</span>
						<strong>${$('<span>').text(c.authorName).html()}</strong>
						<span class="text-muted">${dateStr}</span>
					</div>
					<div class="comment-body mt-1">${$('<span>').text(c.content).html()}</div>
				</div>`);
		});
	};

	// ── 문의 등록 처리 (추후 API 연동) ────────────────────────────────
	$modal.submitWork = async function () {
		const $form    = $('form.workWriteForm', $modal);
		const category = $('select[name=category]', $form).val();
		const title    = $('input[name=title]',     $form).val().trim();
		const content  = editor ? editor.getMarkdown() : '';

		if (!category) { gToast('유형을 선택하세요.', 'warning');   return false; }
		if (!title)    { gToast('제목을 입력하세요.', 'warning');   return false; }
		if (!content.trim()) { gToast('내용을 입력하세요.', 'warning'); return false; }

		const confirmed = await gMessage('문의를 등록하시겠습니까?', '', 'question', 'confirm');
		if (!confirmed.isConfirmed) return false;

		// TODO: dashboard API 연동 후 아래 주석 해제
		// const formData = new FormData();
		// formData.append('category',        category);
		// formData.append('priorityByAgent', $('select[name=priorityByAgent]', $form).val());
		// formData.append('title',           title);
		// formData.append('content',         content);
		// attachFiles.forEach(f => formData.append('files', f));
		// const res = await fetch('/api/dev/works', { method: 'POST', body: formData });
		// if (!res.ok) throw res;

		gToast('등록 기능은 dashboard API 연동 후 활성화됩니다.', 'info');
		return false;
	};

	// ── 댓글 등록 처리 (추후 API 연동) ────────────────────────────────
	$modal.submitComment = async function () {
		const content = $('.commentInput', $modal).val().trim();
		if (!content) { gToast('댓글 내용을 입력하세요.', 'warning'); return; }

		// TODO: dashboard API 연동 후 아래 주석 해제
		// const res = await fetch(`/api/dev/works/${currentWorkId}/comments`, {
		//     method: 'POST',
		//     headers: { 'Content-Type': 'application/json' },
		//     body: JSON.stringify({ content }),
		// });
		// if (!res.ok) throw res;

		gToast('댓글 등록 기능은 dashboard API 연동 후 활성화됩니다.', 'info');
	};

	// ── 첨부파일 선택 ────────────────────────────────────────────────
	$modal.on('change', '.attachInput', function () {
		Array.from(this.files).forEach(f => {
			if (attachFiles.some(ef => ef.name === f.name)) {
				gToast(`중복 파일: ${f.name}`, 'warning'); return;
			}
			if (f.size > MAX_FILE_SIZE) {
				gToast(`파일 크기 초과 (최대 20MB): ${f.name}`, 'warning'); return;
			}
			if (attachFiles.length >= MAX_FILES) {
				gToast(`첨부파일은 최대 ${MAX_FILES}개까지 가능합니다.`, 'warning'); return;
			}
			attachFiles.push(f);
		});
		$(this).val('');
		$modal.renderAttachList();
	});

	// ── 첨부파일 목록 UI 갱신 ─────────────────────────────────────────
	$modal.renderAttachList = function () {
		const $list = $('.attach-file-list', $modal).empty();
		attachFiles.forEach((f, idx) => {
			const sizeKb = (f.size / 1024).toFixed(1);
			$list.append(`
				<li class="d-flex align-items-center" style="gap:6px;font-size:.83rem;">
					<i class="bi bi-file-earmark text-secondary"></i>
					<span>${$('<span>').text(f.name).html()}</span>
					<span class="text-muted">(${sizeKb}KB)</span>
					<button type="button" class="btn btn-sm p-0 btnRemoveAttach text-danger"
						data-idx="${idx}" style="line-height:1;">
						<i class="bi bi-x-circle"></i>
					</button>
				</li>`);
		});
	};

	// ── 이벤트 바인딩 ─────────────────────────────────────────────────

	// 탭 버튼 클릭
	$modal.on('click', '.work-tab-btn', async function () {
		await $modal.switchTab($(this).data('tab'));
	});

	// 첨부파일 삭제
	$modal.on('click', '.btnRemoveAttach', function () {
		attachFiles.splice(Number($(this).data('idx')), 1);
		$modal.renderAttachList();
	});

	// 새 문의 작성 버튼
	$modal.on('click', '.btnGoWrite', async function () {
		await $modal.initWritePanel();
		$modal.showPanel('write');
	});

	// 목록으로 버튼
	$modal.on('click', '.btnBack', function () {
		$modal.showPanel('list');
	});

	// 댓글 등록 버튼
	$modal.on('click', '.btnAddComment', async function () {
		await $modal.submitComment();
	});

	// 문의 목록 행 클릭 → 상세 (더미 데이터, API 연동 전 UI 확인용)
	$modal.on('click', '.work-list-table tbody tr:not(.work-list-empty)', async function () {
		const dummyData = {
			id: 1, category: 'BUG', title: '[UI 확인용] 샘플 문의 제목입니다.',
			content:         '## 문의 내용\n\n이 부분에 문의 본문이 표시됩니다.\n\n- 항목 1\n- 항목 2',
			priorityByAgent: 'NORMAL', workStatus: 'READY',
			createMemberName: G_USER?.name ?? '작성자',
			createdAt:        new Date().toISOString(),
			attachments:      [],
			comments: [
				{ authorType: 'AGENT', authorName: G_USER?.name ?? '사용자',
				  content: '추가로 확인 부탁드립니다.', createdAt: new Date().toISOString() },
				{ authorType: 'DEV',   authorName: '개발팀',
				  content: '확인하였습니다. 처리 예정입니다.', createdAt: new Date().toISOString() },
			],
		};
		await $modal.renderDetail(dummyData);
		$modal.showPanel('detail');
	});

	// ── init_modal ────────────────────────────────────────────────────
	$modal.init_modal = async function (param) {
		$modal.param = param;

		// 기본 진입: 문의 탭, 목록 패널
		$modal.loadWorkList();
		$modal.showPanel('list');

		// workId로 직접 진입 시 상세 패널로 이동
		const initWorkId = $('#initWorkId', $modal).val();
		if (initWorkId && initWorkId !== 'null') {
			// TODO: API 조회 후 renderDetail 호출
			$modal.showPanel('detail');
		}
	};

	// confirm_modal — 푸터 등록 버튼
	$modal.confirm_modal = async function () {
		return await $modal.submitWork();
	};
});
