(() => {
  'use strict';

  const $ = (selector, scope = document) => scope.querySelector(selector);
  const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

  const state = {
    tab: 'home',
    format: 'mp4',
    quality: '1080p',
    audioLang: '',
    downloadDir: 'Downloads/YTDow',
    appVersion: '—',
    tasks: {},
    urlHistory: [],
    downloads: [],
    attempts: [],
    taskState: 'idle',
    animationTimer: null,
    animationNonce: 0,
    sizeTimer: null,
    taskSyncTimer: null,
    taskCleanupTimers: {},
    lastSizeBytes: 0,
    lastSizeLabel: '',
    sheetTrigger: null,
  };

  const mascotAnimations = {
    downloading: { src: 'animations/dance.webp', blobUrl: '', durationMs: 2000, loop: true },
    success: { src: 'animations/success.webp', blobUrl: '', durationMs: 2417, loop: false },
    errorIntro: { src: 'animations/error-intro.webp', blobUrl: '', durationMs: 1083, loop: false },
    errorLoop: { src: 'animations/error-loop.webp', blobUrl: '', durationMs: 1083, loop: true },
  };

  async function prepareMascotAnimations() {
    await Promise.all(Object.values(mascotAnimations).map(async animation => {
      try {
        const response = await fetch(animation.src, { cache: 'force-cache' });
        if (!response.ok) return;
        animation.blobUrl = URL.createObjectURL(await response.blob());
      } catch (_) {
        // The original appassets URL remains a safe fallback.
      }
    }));
  }

  function callNative(method, fallback, ...args) {
    try {
      const bridge = window.Android;
      if (!bridge || typeof bridge[method] !== 'function') return fallback;
      const result = bridge[method](...args);
      return result === undefined || result === null ? fallback : result;
    } catch (error) {
      console.error(`Android.${method} failed`, error);
      return fallback;
    }
  }

  function parseArray(value) {
    try {
      const parsed = JSON.parse(value || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch (_) {
      return [];
    }
  }

  function normalizeEntries(entries) {
    return entries.map(entry => {
      if (typeof entry !== 'string') return entry;
      try { return JSON.parse(entry); } catch (_) { return null; }
    }).filter(Boolean);
  }

  function escapeHtml(value) {
    const node = document.createElement('div');
    node.textContent = String(value ?? '');
    return node.innerHTML;
  }

  function escapeAttr(value) {
    return escapeHtml(value).replace(/`/g, '&#96;');
  }

  function plural(count, one, few, many) {
    const mod10 = count % 10;
    const mod100 = count % 100;
    if (mod10 === 1 && mod100 !== 11) return one;
    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
    return many;
  }

  function formatBytes(bytes) {
    const value = Number(bytes) || 0;
    if (value <= 0) return '—';
    const unit = value >= 1024 ** 3 ? 'ГБ' : 'МБ';
    const divisor = unit === 'ГБ' ? 1024 ** 3 : 1024 ** 2;
    return `${(value / divisor).toFixed(1).replace('.', ',')} ${unit}`;
  }

  function timeAgo(timestamp) {
    const value = Number(timestamp) || 0;
    if (!value) return 'недавно';
    const seconds = Math.max(0, (Date.now() - value) / 1000);
    if (seconds < 60) return 'только что';
    if (seconds < 3600) return `${Math.floor(seconds / 60)} мин назад`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)} ч назад`;
    if (seconds < 172800) return 'вчера';
    return `${Math.floor(seconds / 86400)} дн назад`;
  }

  function folderFromPath(filePath) {
    const path = String(filePath || '');
    const index = path.lastIndexOf('/');
    return index > 0 ? path.slice(0, index) : state.downloadDir;
  }

  function showToast(message) {
    const toast = $('#toast');
    toast.textContent = message;
    toast.classList.add('show');
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove('show'), 2400);
  }

  function hasActiveTasks() {
    return Object.values(state.tasks).some(task => task.status === 'queued' || task.status === 'downloading');
  }

  function playMascotAnimation(name, onComplete) {
    const mascot = $('#mascot');
    const animation = mascotAnimations[name];
    const playId = ++state.animationNonce;
    clearTimeout(state.animationTimer);
    mascot.dataset.animation = name.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`);
    mascot.dataset.animationReady = 'false';

    mascot.onload = () => {
      if (playId !== state.animationNonce) return;
      mascot.dataset.animationReady = 'true';
      if (!animation.loop && onComplete) {
        state.animationTimer = setTimeout(() => {
          if (playId === state.animationNonce) onComplete();
        }, animation.durationMs);
      }
    };
    mascot.onerror = () => {
      if (playId !== state.animationNonce) return;
      mascot.dataset.animationReady = 'error';
      mascot.removeAttribute('src');
      mascot.dataset.animationReady = 'false';
      showToast('Не удалось загрузить анимацию персонажа');
    };

    mascot.removeAttribute('src');
    void mascot.offsetWidth;
    const source = animation.blobUrl || animation.src;
    mascot.src = source.startsWith('blob:')
      ? `${source}#play=${playId}`
      : `${source}${source.includes('?') ? '&' : '?'}play=${playId}`;
  }

  function setMascotState(next) {
    const stage = $('#mascot-stage');
    const mascot = $('#mascot');
    clearTimeout(state.animationTimer);
    state.taskState = next;

    if (next === 'idle') {
      state.animationNonce += 1;
      mascot.removeAttribute('src');
      mascot.dataset.animation = 'idle';
      mascot.dataset.animationReady = 'false';
      mascot.alt = '';
      stage.classList.add('hidden');
      return;
    }

    stage.classList.remove('hidden');
    if (next === 'downloading') {
      mascot.alt = 'Альтушка плавно танцует во время загрузки';
      playMascotAnimation('downloading');
      return;
    }

    if (next === 'success') {
      mascot.alt = 'Альтушка показывает победный жест и отправляет воздушный поцелуй';
      playMascotAnimation('success', () => {
        if (state.taskState !== 'success') return;
        if (hasActiveTasks()) setMascotState('downloading');
        else setMascotState('idle');
      });
      return;
    }

    if (next === 'error') {
      mascot.alt = 'Расстроенная альтушка плачет из-за ошибки загрузки';
      playMascotAnimation('errorIntro', () => {
        if (state.taskState === 'error') playMascotAnimation('errorLoop');
      });
    }
  }

  function switchTab(tab) {
    state.tab = tab;
    $$('.page').forEach(page => page.classList.toggle('active', page.dataset.page === tab));
    $$('.nav-button').forEach(button => {
      const active = button.dataset.tab === tab;
      button.classList.toggle('active', active);
      button.setAttribute('aria-selected', String(active));
    });
    if (tab === 'library') refreshDownloads();
    if (tab === 'history') refreshAttempts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function openSheet(name) {
    closeSheets(false);
    state.sheetTrigger = document.activeElement;
    const sheet = $(`#sheet-${name}`);
    $('#overlay').classList.add('show');
    sheet.classList.add('show');
    sheet.setAttribute('aria-hidden', 'false');
    $('.app').inert = true;
    $('.bottom-nav').inert = true;
    document.body.style.overflow = 'hidden';
    requestAnimationFrame(() => sheet.focus());
  }

  function closeSheets(restoreFocus = true) {
    $('#overlay').classList.remove('show');
    $$('.sheet').forEach(sheet => {
      sheet.classList.remove('show');
      sheet.setAttribute('aria-hidden', 'true');
    });
    $('.app').inert = false;
    $('.bottom-nav').inert = false;
    document.body.style.overflow = '';
    if (restoreFocus && state.sheetTrigger instanceof HTMLElement) state.sheetTrigger.focus();
    if (restoreFocus) state.sheetTrigger = null;
  }

  function closeActiveSheet() {
    if (!$('.sheet.show')) return false;
    closeSheets();
    return true;
  }

  function handleDialogKeys(event) {
    const sheet = $('.sheet.show');
    if (!sheet) return;
    if (event.key === 'Escape') {
      event.preventDefault();
      closeSheets();
      return;
    }
    if (event.key !== 'Tab') return;
    const focusable = $$('button:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])', sheet)
      .filter(element => !element.hidden);
    if (!focusable.length) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  function resetSizeResult() {
    clearTimeout(state.sizeTimer);
    state.lastSizeBytes = 0;
    state.lastSizeLabel = '';
    const button = $('#size-button');
    button.classList.remove('loading', 'resolved');
    button.setAttribute('aria-label', 'Узнать размер файла');
    $('#size-text').textContent = 'Узнать размер';
  }

  function setChoice(type, value, label, button) {
    state[type] = value;
    $(`#${type}-value`).textContent = label;
    $$(`[data-choice="${type}"]`).forEach(option => {
      const selected = option === button;
      option.classList.toggle('selected', selected);
      option.setAttribute('aria-checked', String(selected));
    });
    if (type === 'format') $('#settings-format').textContent = label;
    resetSizeResult();
    closeSheets();
    showToast(`${type === 'format' ? 'Формат' : 'Качество'}: ${label}`);
  }

  function renderUrlHistory() {
    const container = $('#url-history');
    container.innerHTML = state.urlHistory.slice().reverse().map((url, index) =>
      `<button class="history-suggestion" type="button" data-url-index="${index}">${escapeHtml(url)}</button>`
    ).join('');
    if (!container.children.length) container.classList.remove('show');
  }

  function showUrlHistory() {
    if (!state.urlHistory.length || $('#url-input').value.trim()) return;
    $('#url-history').classList.add('show');
  }

  function refreshUrlHistory() {
    state.urlHistory = parseArray(callNative('getHistory', '[]'));
    renderUrlHistory();
  }

  function refreshDownloads() {
    state.downloads = normalizeEntries(parseArray(callNative('getDownloadHistory', '[]'))).reverse();
    renderLibrary();
    renderHistory();
  }

  function refreshAttempts() {
    state.attempts = normalizeEntries(parseArray(callNative('getAttemptHistory', '[]'))).reverse();
    renderHistory();
  }

  function scheduleTaskRemoval(taskId, expectedStatus, delayMs) {
    if (state.taskCleanupTimers[taskId]) return;
    state.taskCleanupTimers[taskId] = setTimeout(() => {
      delete state.taskCleanupTimers[taskId];
      if (state.tasks[taskId]?.status !== expectedStatus) return;
      delete state.tasks[taskId];
      renderTasks();
    }, delayMs);
  }

  function syncTasksFromAttempts() {
    const attempts = normalizeEntries(parseArray(callNative('getAttemptHistory', '[]')));
    if (!attempts.length) return;
    state.attempts = attempts.slice().reverse();
    let changed = false;
    let terminalChange = false;

    attempts.forEach(attempt => {
      const taskId = String(attempt.taskId || '');
      if (!taskId) return;
      const persistedStatus = attempt.status === 'failed' ? 'error' : attempt.status;
      let task = state.tasks[taskId];
      if (!task && ['queued', 'downloading'].includes(persistedStatus)) {
        task = {
          id: taskId,
          url: String(attempt.url || ''),
          title: String(attempt.title || 'Подготовка загрузки'),
          format: String(attempt.format || 'mp4'),
          quality: String(attempt.quality || 'best'),
          status: persistedStatus,
          percent: Number.isFinite(Number(attempt.percent)) ? Number(attempt.percent) : 0,
          speed: String(attempt.speed || ''),
          filePath: String(attempt.filePath || ''),
          sizeBytes: Number(attempt.sizeBytes) || 0,
          error: '',
        };
        state.tasks[taskId] = task;
        changed = true;
      }
      if (!task) return;

      const previousStatus = task.status;
      const nextStatus = persistedStatus;
      if (['completed', 'error', 'cancelled'].includes(previousStatus) &&
          ['queued', 'downloading'].includes(nextStatus)) return;
      if (['queued', 'downloading', 'completed', 'error', 'cancelled'].includes(nextStatus)) {
        task.status = nextStatus;
      }
      if (Number.isFinite(Number(attempt.percent))) task.percent = Number(attempt.percent);
      if (attempt.speed) task.speed = String(attempt.speed);
      if (attempt.title) task.title = String(attempt.title);
      if (attempt.filePath) task.filePath = String(attempt.filePath);
      if (Number(attempt.sizeBytes) > 0) task.sizeBytes = Number(attempt.sizeBytes);
      if (attempt.error) task.error = String(attempt.error);

      if (task.status === 'completed') {
        task.percent = 100;
        scheduleTaskRemoval(taskId, 'completed', 2800);
      }
      if (task.status === 'cancelled') {
        scheduleTaskRemoval(taskId, 'cancelled', 3200);
      }
      if (task.status !== previousStatus || task.status === 'downloading') changed = true;
      if (task.status !== previousStatus && ['completed', 'error', 'cancelled'].includes(task.status)) {
        terminalChange = true;
        if (task.status === 'completed') setMascotState('success');
        if (task.status === 'cancelled' || task.status === 'error') setMascotState('error');
      }
    });

    if (terminalChange) refreshDownloads();
    if (changed) {
      renderTasks();
      renderHistory();
    }
  }

  function startDownload() {
    const input = $('#url-input');
    const urls = input.value.split(',').map(value => value.trim()).filter(value => /^https?:\/\//i.test(value));
    if (!urls.length) {
      showToast('Вставьте корректную ссылку');
      input.focus();
      return;
    }

    const estimatedSize = urls.length === 1 ? state.lastSizeBytes : 0;
    let started = 0;
    urls.forEach(url => {
      const taskId = callNative('startDownload', '', url, state.format, state.quality, state.audioLang);
      if (!taskId) return;
      state.tasks[taskId] = {
        id: taskId,
        url,
        title: 'Подготовка загрузки',
        format: state.format,
        quality: state.quality,
        status: 'queued',
        percent: 0,
        speed: '',
        filePath: '',
        sizeBytes: estimatedSize,
        error: '',
      };
      started += 1;
    });

    if (!started) {
      showToast('Не удалось запустить загрузку');
      return;
    }

    input.value = '';
    $('#url-history').classList.remove('show');
    refreshUrlHistory();
    renderTasks();
    setMascotState('downloading');
    showToast(started > 1 ? `Запущено загрузок: ${started}` : 'Загрузка началась');
  }

  function checkSize() {
    const raw = $('#url-input').value.trim();
    const url = raw.split(',').map(value => value.trim()).find(value => /^https?:\/\//i.test(value));
    if (!url) {
      showToast('Сначала вставьте ссылку');
      return;
    }

    const button = $('#size-button');
    button.classList.remove('resolved');
    button.classList.add('loading');
    button.setAttribute('aria-label', 'Рассчитываем размер файла');
    callNative('checkSize', undefined, url, state.format, state.quality, state.audioLang);
    clearTimeout(state.sizeTimer);
    state.sizeTimer = setTimeout(() => {
      if (button.classList.contains('loading')) onSizeError('Превышено время ожидания');
    }, 30000);
  }

  function onSizeResult(payload) {
    clearTimeout(state.sizeTimer);
    let data;
    try { data = JSON.parse(payload); } catch (_) { data = {}; }
    const bytes = Number(data.sizeBytes) || 0;
    const button = $('#size-button');
    button.classList.remove('loading');
    button.classList.add('resolved');
    state.lastSizeBytes = bytes;
    state.lastSizeLabel = formatBytes(bytes);
    $('#size-text').textContent = state.lastSizeLabel;
    button.setAttribute('aria-label', bytes > 0 ? `Размер файла ${state.lastSizeLabel}` : 'Размер файла неизвестен');
  }

  function onSizeError() {
    clearTimeout(state.sizeTimer);
    const button = $('#size-button');
    button.classList.remove('loading');
    button.classList.add('resolved');
    $('#size-text').textContent = '—';
    button.setAttribute('aria-label', 'Не удалось определить размер файла');
  }

  function taskTitle(task) {
    if (task.title && task.title !== 'Подготовка загрузки' && task.title !== 'Ожидание...') return task.title;
    try {
      const parsed = new URL(task.url);
      return parsed.hostname.replace(/^www\./, '');
    } catch (_) {
      return task.url || 'Видео';
    }
  }

  function taskStatus(task) {
    if (task.status === 'completed') return '<div class="task-status state-success">Готово</div>';
    if (task.status === 'error') return `<div class="task-status state-error">${escapeHtml(task.error || 'Ошибка загрузки')}</div>`;
    if (task.status === 'cancelled') return '<div class="task-status state-cancelled">Отменено</div>';
    if (task.status === 'queued') return '<div class="task-status">Подготовка…</div>';
    const percent = Number(task.percent);
    return percent >= 0
      ? `<div class="task-status">Загрузка… <strong>${percent}%</strong></div>`
      : '<div class="task-status">Загрузка…</div>';
  }

  function renderTasks() {
    const container = $('#task-list');
    const taskIds = Object.keys(state.tasks).reverse();
    if (!taskIds.length) {
      container.innerHTML = '<div class="empty-state">Нет активных загрузок</div>';
      return;
    }

    container.innerHTML = taskIds.map(taskId => {
      const task = state.tasks[taskId];
      const percent = Number(task.percent);
      const indeterminate = task.status === 'queued' || (task.status === 'downloading' && percent < 0);
      const width = task.status === 'completed' ? 100 : task.status === 'error' || task.status === 'cancelled' ? Math.max(8, percent || 0) : Math.max(0, percent || 0);
      const active = task.status === 'queued' || task.status === 'downloading';
      const size = formatBytes(task.sizeBytes);
      const cardClass = ['completed', 'error', 'cancelled'].includes(task.status) ? ` ${task.status}` : '';
      let actions = '';
      if (task.status === 'completed') {
        actions = `<div class="task-actions"><button class="soft-button" type="button" data-task-action="open" data-task-id="${escapeAttr(taskId)}"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#i-play"/></svg><span>Открыть</span></button><button class="soft-button" type="button" data-task-action="folder" data-task-id="${escapeAttr(taskId)}"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#i-folder"/></svg><span>Папка</span></button></div>`;
      } else if (task.status === 'error') {
        actions = `<div class="task-actions"><button class="soft-button" type="button" data-task-action="retry" data-task-id="${escapeAttr(taskId)}"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#i-refresh"/></svg><span>Повторить</span></button></div>`;
      }

      return `<article class="task-card${cardClass}">
        <div class="task-top">
          <div class="task-icon"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#${task.format === 'mp3' ? 'i-file' : 'i-download'}"/></svg></div>
          <div class="task-copy"><div class="task-title">${escapeHtml(taskTitle(task))}</div>${taskStatus(task)}</div>
          ${active ? `<button class="cancel-button" type="button" data-task-action="cancel" data-task-id="${escapeAttr(taskId)}" aria-label="Отменить загрузку"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#i-x"/></svg></button>` : '<div></div>'}
        </div>
        <div class="progress-track"><div class="progress-fill${indeterminate ? ' indeterminate' : ''}" style="width:${width}%"></div></div>
        <div class="stats">
          <div class="stat"><svg fill="none" stroke="currentColor" stroke-width="1.8"><use href="#i-speed"/></svg><div><span>Скорость</span><strong>${escapeHtml(task.status === 'completed' ? 'Завершено' : task.status === 'error' || task.status === 'cancelled' ? 'Остановлено' : task.speed || 'Подготовка')}</strong></div></div>
          <div class="stat"><svg fill="none" stroke="currentColor" stroke-width="1.8"><use href="#i-file"/></svg><div><span>Размер файла</span><strong>${size}</strong></div></div>
        </div>${actions}
      </article>`;
    }).join('');
  }

  function renderLibrary() {
    const count = state.downloads.length;
    $('#library-count').textContent = `${count} ${plural(count, 'файл', 'файла', 'файлов')}`;
    const container = $('#library-list');
    if (!count) {
      container.innerHTML = '<div class="empty-state">Скачанные файлы появятся здесь</div>';
      return;
    }

    container.innerHTML = state.downloads.map((entry, index) => {
      const format = String(entry.format || 'mp4').toUpperCase();
      const icon = entry.format === 'mp3' ? 'i-file' : 'i-video';
      return `<article class="file-card">
        <div class="file-main"><div class="file-thumb"><svg fill="none" stroke="currentColor" stroke-width="1.8"><use href="#${icon}"/></svg></div><div><div class="file-title">${escapeHtml(entry.title || entry.url || 'Файл')}</div><div class="file-meta">${escapeHtml(format)} · ${escapeHtml(entry.quality || 'best')} · ${formatBytes(entry.sizeBytes)} · ${timeAgo(entry.time)}</div></div></div>
        <div class="file-actions">
          <button class="soft-button" type="button" data-library-action="open" data-index="${index}"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#i-play"/></svg><span>Открыть</span></button>
          <button class="soft-button" type="button" data-library-action="folder" data-index="${index}"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#i-folder"/></svg><span>Папка</span></button>
          <button class="soft-button" type="button" data-library-action="delete" data-index="${index}"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#i-trash"/></svg><span>Удалить</span></button>
        </div>
      </article>`;
    }).join('');
  }

  function mergedHistory() {
    const result = state.attempts.map(entry => ({ ...entry }));
    const paths = new Set(result.map(entry => entry.filePath).filter(Boolean));
    state.downloads.forEach(entry => {
      if (entry.filePath && paths.has(entry.filePath)) return;
      result.push({ ...entry, status: 'completed' });
    });
    return result.sort((left, right) => Number(right.time || right.finishedAt || 0) - Number(left.time || left.finishedAt || 0));
  }

  function renderHistory() {
    const entries = mergedHistory();
    const count = entries.length;
    $('#history-count').textContent = `${count} ${plural(count, 'запись', 'записи', 'записей')}`;
    const container = $('#history-list');
    if (!count) {
      container.innerHTML = '<div class="empty-state">История загрузок пока пуста</div>';
      return;
    }

    container.innerHTML = entries.map(entry => {
      const status = entry.status === 'failed' ? 'error' : entry.status || 'completed';
      const statusClass = status === 'completed' ? 'success' : status === 'cancelled' ? 'cancelled' : status === 'error' ? 'error' : 'queued';
      const icon = statusClass === 'success' ? 'i-check' : statusClass === 'cancelled' ? 'i-x' : statusClass === 'error' ? 'i-alert' : 'i-refresh';
      const statusLabel = statusClass === 'success' ? formatBytes(entry.sizeBytes) : statusClass === 'cancelled' ? 'Отменено' : statusClass === 'error' ? 'Ошибка' : 'В очереди';
      const details = [timeAgo(entry.time || entry.finishedAt), String(entry.format || 'mp4').toUpperCase(), entry.quality || 'best'];
      if (statusClass === 'error' && entry.error) details.push(entry.error);
      return `<article class="history-card"><div class="history-state ${statusClass}"><svg fill="none" stroke="currentColor" stroke-width="2"><use href="#${icon}"/></svg></div><div><div class="history-title">${escapeHtml(entry.title || entry.url || 'Загрузка')}</div><div class="history-meta">${details.map(escapeHtml).join(' · ')}</div></div><div class="history-size">${escapeHtml(statusLabel)}</div></article>`;
    }).join('');
  }

  function cancelDownload(taskId) {
    const task = state.tasks[taskId];
    if (!task) return;
    callNative('cancelDownload', undefined, taskId);
    task.status = 'cancelled';
    renderTasks();
    setMascotState('error');
    scheduleTaskRemoval(taskId, 'cancelled', 3200);
    showToast('Загрузка отменена');
    setTimeout(refreshAttempts, 180);
  }

  function retryDownload(taskId) {
    const previous = state.tasks[taskId];
    if (!previous) return;
    const nextId = callNative('startDownload', '', previous.url, previous.format, previous.quality, state.audioLang);
    if (!nextId) return showToast('Не удалось повторить загрузку');
    state.tasks[nextId] = { ...previous, id: nextId, status: 'queued', percent: 0, speed: '', error: '', filePath: '' };
    delete state.tasks[taskId];
    renderTasks();
    setMascotState('downloading');
    showToast('Повторная загрузка началась');
  }

  function openTaskFile(taskId) {
    const task = state.tasks[taskId];
    if (task?.filePath) callNative('openFile', undefined, task.filePath);
    else showToast('Файл не найден');
  }

  function openTaskFolder(taskId) {
    callNative('openFolder', undefined, state.downloadDir);
  }

  function handleLibraryAction(action, index) {
    const entry = state.downloads[index];
    if (!entry) return;
    if (action === 'open') return callNative('openFile', undefined, entry.filePath);
    if (action === 'folder') return callNative('openFolder', undefined, state.downloadDir);
    if (action === 'delete' && window.confirm('Удалить файл?')) {
      callNative('deleteFile', undefined, entry.filePath);
      showToast('Удаляем файл…');
    }
  }

  function onDeleteResult(filePath, success) {
    if (!success) {
      showToast('Не удалось удалить файл');
      return;
    }
    state.downloads = state.downloads.filter(entry => entry.filePath !== filePath);
    refreshDownloads();
    refreshAttempts();
    renderLibrary();
    renderHistory();
    showToast('Файл удалён');
  }

  function onProgress(taskId, percent, speed) {
    const existingTask = state.tasks[taskId];
    if (existingTask && ['completed', 'error', 'cancelled'].includes(existingTask.status)) return;
    const task = existingTask || {
      id: taskId, url: '', title: 'Видео загружается', format: 'mp4', quality: 'best', sizeBytes: 0, filePath: '', error: '',
    };
    task.status = 'downloading';
    task.percent = Number(percent);
    if (speed) task.speed = speed;
    state.tasks[taskId] = task;
    renderTasks();
    if (state.taskState === 'idle') setMascotState('downloading');
  }

  function onComplete(taskId, filePath) {
    const task = state.tasks[taskId] || {
      id: taskId, url: '', title: '', format: 'mp4', quality: 'best', sizeBytes: 0, speed: '', error: '',
    };
    task.status = 'completed';
    task.percent = 100;
    task.filePath = filePath || task.filePath || '';
    state.tasks[taskId] = task;
    refreshDownloads();
    refreshAttempts();
    const historyMatch = state.downloads.find(entry => entry.filePath === task.filePath);
    if (historyMatch) {
      task.title = historyMatch.title || task.title;
      task.sizeBytes = historyMatch.sizeBytes || task.sizeBytes;
    }
    renderTasks();
    setMascotState('success');
    scheduleTaskRemoval(taskId, 'completed', 2800);
  }

  function onError(taskId, error) {
    const task = state.tasks[taskId] || {
      id: taskId, url: '', title: '', format: 'mp4', quality: 'best', sizeBytes: 0, speed: '', filePath: '',
    };
    task.status = 'error';
    task.error = error || 'Ошибка загрузки';
    state.tasks[taskId] = task;
    refreshAttempts();
    renderTasks();
    setMascotState('error');
  }

  function onHistoryChanged() {
    refreshDownloads();
    refreshAttempts();
    renderTasks();
  }

  function checkUpdate() {
    const status = $('#update-status');
    const button = $('#update-button');
    status.textContent = 'Проверяем обновления…';
    status.classList.remove('error');
    button.disabled = true;
    button.textContent = 'Проверяем…';
    callNative('checkUpdate', undefined);
  }

  function onUpdateResult(resultValue) {
    const status = $('#update-status');
    let result;
    try { result = JSON.parse(resultValue); } catch (_) { result = { error: 'Некорректный ответ' }; }
    const updateButton = $('#update-button');
    updateButton.disabled = false;
    updateButton.textContent = 'Проверить обновление';
    status.classList.remove('error');
    status.replaceChildren();
    if (result.error) {
      status.textContent = `Ошибка: ${result.error}`;
      status.classList.add('error');
      return;
    }
    if (!result.hasUpdate) {
      status.textContent = `Установлена последняя версия (${result.current || state.appVersion})`;
      return;
    }
    if (!result.downloadUrl) {
      status.textContent = `Доступна версия ${result.latest}, APK пока не опубликован`;
      return;
    }
    const text = document.createTextNode(`Доступна версия ${result.latest} `);
    const button = document.createElement('button');
    button.className = 'soft-button';
    button.type = 'button';
    button.textContent = 'Скачать и установить';
    button.addEventListener('click', () => downloadUpdate(result.downloadUrl));
    status.append(text, button);
  }

  function downloadUpdate(url) {
    $('#update-status').textContent = 'Скачивание обновления… 0%';
    $('#update-button').disabled = true;
    callNative('downloadUpdate', undefined, url);
  }

  function onUpdateProgress(percent) {
    $('#update-status').textContent = `Скачивание обновления… ${Number(percent) || 0}%`;
  }

  function onUpdateStatus(message, isError) {
    const status = $('#update-status');
    status.textContent = String(message || '');
    status.classList.toggle('error', Boolean(isError));
    $('#update-button').disabled = false;
  }

  function initialize() {
    state.downloadDir = callNative('getDownloadDir', 'Downloads/YTDow');
    state.appVersion = callNative('getAppVersion', '—');
    $('#folder-button span').textContent = state.downloadDir;
    $('#settings-path').textContent = state.downloadDir;
    $('#settings-format').textContent = 'MP4';
    $('#app-version').textContent = state.appVersion;
    refreshUrlHistory();
    refreshDownloads();
    refreshAttempts();
    renderTasks();
    setMascotState('idle');
    $$('[data-choice]').forEach(button => {
      button.setAttribute('role', 'radio');
      button.setAttribute('aria-checked', String(button.classList.contains('selected')));
    });

    prepareMascotAnimations();

    $$('[data-tab]').forEach(button => button.addEventListener('click', () => switchTab(button.dataset.tab)));
    $$('[data-open-sheet]').forEach(button => button.addEventListener('click', () => openSheet(button.dataset.openSheet)));
    $$('[data-choice]').forEach(button => button.addEventListener('click', () => setChoice(button.dataset.choice, button.dataset.value, button.dataset.label, button)));
    $$('[data-close-sheet]').forEach(button => button.addEventListener('click', () => closeSheets()));
    $('#overlay').addEventListener('click', () => closeSheets());
    document.addEventListener('keydown', handleDialogKeys);
    $('#download-button').addEventListener('click', startDownload);
    $('#size-button').addEventListener('click', checkSize);
    $('#folder-button').addEventListener('click', () => callNative('openFolder', undefined, state.downloadDir));
    $('#clear-url').addEventListener('click', () => { $('#url-input').value = ''; $('#url-input').focus(); showUrlHistory(); });
    $('#url-input').addEventListener('keydown', event => { if (event.key === 'Enter') startDownload(); });
    $('#url-input').addEventListener('input', () => { resetSizeResult(); $('#url-history').classList.remove('show'); });
    $('#url-input').addEventListener('focus', showUrlHistory);
    $('#url-input').addEventListener('blur', () => setTimeout(() => $('#url-history').classList.remove('show'), 160));
    $('#url-history').addEventListener('click', event => {
      const button = event.target.closest('[data-url-index]');
      if (!button) return;
      const reversed = state.urlHistory.slice().reverse();
      $('#url-input').value = reversed[Number(button.dataset.urlIndex)] || '';
      $('#url-history').classList.remove('show');
      resetSizeResult();
    });
    $('#task-list').addEventListener('click', event => {
      const button = event.target.closest('[data-task-action]');
      if (!button) return;
      const taskId = button.dataset.taskId;
      if (button.dataset.taskAction === 'cancel') cancelDownload(taskId);
      if (button.dataset.taskAction === 'retry') retryDownload(taskId);
      if (button.dataset.taskAction === 'open') openTaskFile(taskId);
      if (button.dataset.taskAction === 'folder') openTaskFolder(taskId);
    });
    $('#library-list').addEventListener('click', event => {
      const button = event.target.closest('[data-library-action]');
      if (button) handleLibraryAction(button.dataset.libraryAction, Number(button.dataset.index));
    });
    $('#update-button').addEventListener('click', checkUpdate);
    $('#legal-button').addEventListener('click', () => { window.location.href = 'legal.html'; });
    clearInterval(state.taskSyncTimer);
    state.taskSyncTimer = setInterval(syncTasksFromAttempts, 1000);
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) syncTasksFromAttempts();
    });
    syncTasksFromAttempts();
  }

  Object.assign(window, {
    onProgress,
    onComplete,
    onError,
    onSizeResult,
    onSizeError,
    onHistoryChanged,
    onDeleteResult,
    onUpdateResult,
    onUpdateProgress,
    onUpdateStatus,
    YTDowApp: { state, switchTab, setMascotState, refreshDownloads, refreshAttempts, syncTasksFromAttempts, closeActiveSheet },
  });

  document.addEventListener('DOMContentLoaded', initialize, { once: true });
})();
