/*
 * Watsup custom mobile chat UI — bridge to the official WhatsApp Web engine
 * through WPPConnect wa-js v4.3.0 (verified API, source-checked):
 *
 *   WPP.chat.list()                       -> Promise<ChatModel[]>
 *   WPP.chat.getMessages(chatId, opts)    -> Promise<RawMessage[]>
 *   WPP.chat.sendTextMessage(chatId, txt) -> Promise<SendMessageReturn>
 *   WPP.chat.markIsRead(chatId)           -> Promise<{wid, unreadCount}>
 *   WPP.ev.on('chat.new_message', msg)
 *   WPP.ev.on('chat.unread_count_changed', ev)
 *   WPP.ev.on('chat.msg_revoke', ev)
 *   WPP.ev.on('chat.msg_ack_change', ev)
 *   WPP.ev.on('chat.new_chat', chat)
 *
 * All rendering is XSS-safe (createElement + textContent, never innerHTML
 * with remote data). Runs inside the live web.whatsapp.com page; the page
 * keeps running untouched underneath (hidden by custom-ui.css).
 */
(function () {
  'use strict';
  if (window.__waUi) { return; }
  window.__waUi = true;

  // ---------- DOM ----------
  var viewList = document.getElementById('wa-view-list');
  var viewChat = document.getElementById('wa-view-chat');
  var listEl = document.getElementById('wa-chatlist');
  var searchEl = document.getElementById('wa-search-input');
  var msgsEl = document.getElementById('wa-msgs');
  var inputEl = document.getElementById('wa-input');
  var sendBtn = document.getElementById('wa-btn-send');
  var backBtn = document.getElementById('wa-btn-back');
  var markReadBtn = document.getElementById('wa-btn-markread');
  var logoutBtn = document.getElementById('wa-btn-logout');
  var chatTitleEl = document.getElementById('wa-chat-title');
  var chatSubEl = document.getElementById('wa-chat-sub');
  var chatAvatarEl = document.getElementById('wa-chat-avatar');
  var toastEl = document.getElementById('wa-toast');

  // ---------- state ----------
  var chats = [];
  var chatById = Object.create(null);
  var activeChat = null;      // { id, title, isGroup }
  var rendered = Object.create(null);  // msg key -> true (for active chat)
  var bubbleById = Object.create(null); // msg key -> bubble element
  var pendingSends = [];      // { body, el, key }
  var searchTerm = '';
  var refreshScheduled = false;
  var toastTimer = null;
  var logoutArmed = false;
  var logoutTimer = null;

  var SKIP_TYPES = {
    call: 1, ciphertext: 1, e2e_notification: 1, notification: 1, revoke: 1,
    revoked: 1, reaction: 1, reaction_notification: 1, receipt: 1, payment: 1,
    proto: 1, gp2: 1, appended: 1, order: 1, button: 1, source: 1,
    system: 1, creator_intent: 1, schedule_call: 1
  };

  var AVATAR_COLORS = [
    '#e91e63', '#9c27b0', '#673ab7', '#3f51b5', '#2196f3', '#009688',
    '#4caf50', '#ff9800', '#ff5722', '#795548', '#607d8b', '#c2185b',
    '#5d4037', '#455a64'
  ];

  // ---------- small helpers ----------
  function wid(w) {
    if (!w) { return null; }
    if (typeof w === 'string') { return w; }
    if (w._serialized) { return w._serialized; }
    try {
      if (w.toString) {
        var s = w.toString();
        if (s && s !== '[object Object]') { return s; }
      }
    } catch (e) {}
    return null;
  }

  function shortWid(w) {
    var s = wid(w);
    if (!s) { return null; }
    return s.split('@')[0];
  }

  function msgKey(m) {
    try {
      if (m.id) { return m.id._serialized || m.id.id || null; }
    } catch (e) {}
    return null;
  }

  function msgChatId(m) {
    try { if (m.id && m.id.remote) { return wid(m.id.remote); } } catch (e) {}
    try { if (m.chatId) { return wid(m.chatId); } } catch (e) {}
    try {
      if (!m.fromMe && m.from) { return wid(m.from); }
      if (m.fromMe && m.to) { return wid(m.to); }
    } catch (e) {}
    return null;
  }

  function pad(n) { return n < 10 ? '0' + n : '' + n; }

  function dayKey(ms) {
    var d = new Date(ms);
    return d.getFullYear() + '-' + d.getMonth() + '-' + d.getDate();
  }

  function fmtClock(ms) {
    var d = new Date(ms);
    return pad(d.getHours()) + ':' + pad(d.getMinutes());
  }

  function fmtDayLabel(ms) {
    var now = Date.now();
    if (dayKey(ms) === dayKey(now)) { return 'Today'; }
    if (dayKey(ms) === dayKey(now - 86400000)) { return 'Yesterday'; }
    var d = new Date(ms);
    return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear();
  }

  function fmtListTime(ms) {
    if (!ms) { return ''; }
    if (dayKey(ms) === dayKey(Date.now())) { return fmtClock(ms); }
    if (ms >= Date.now() - 7 * 86400000) {
      return ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][new Date(ms).getDay()];
    }
    var d = new Date(ms);
    return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + String(d.getFullYear()).slice(2);
  }

  function initials(name) {
    var s = (name || '').trim();
    if (!s) { return '?'; }
    var parts = s.split(/\s+/);
    var a = parts[0].charAt(0);
    var b = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
    return (a + b).toUpperCase() || '?';
  }

  function avatarColor(name) {
    var h = 0, s = String(name || '');
    for (var i = 0; i < s.length; i++) { h = (h * 31 + s.charCodeAt(i)) >>> 0; }
    return AVATAR_COLORS[h % AVATAR_COLORS.length];
  }

  function mediaLabel(type) {
    switch (type) {
      case 'image': return '📷 Photo';
      case 'video': return '🎬 Video';
      case 'ptt': return '🎤 Voice message';
      case 'audio': return '🎵 Audio';
      case 'document': return '📎 Document';
      case 'sticker': return '🏷 Sticker';
      case 'location':
      case 'location_v2': return '📍 Location';
      case 'vcard':
      case 'multi_vcard':
      case 'multiple_vcards': return '👤 Contact card';
      case 'poll_creation': return '📊 Poll';
      case 'interactive':
      case 'native_flow': return '⚙ Interactive message';
      case 'product':
      case 'catalog':
      case 'order': return '🛍 Order';
      case 'template_button_reply': return null; // body carries the reply text
      default: return null;
    }
  }

  function toast(text) {
    if (!toastEl) { return; }
    toastEl.textContent = text;
    toastEl.hidden = false;
    if (toastTimer) { clearTimeout(toastTimer); }
    toastTimer = setTimeout(function () { toastEl.hidden = true; }, 2600);
  }

  function showListState(text) {
    listEl.innerHTML = '';
    var d = document.createElement('div');
    d.className = 'wa-state';
    d.textContent = text;
    listEl.appendChild(d);
  }

  function showMsgsState(text) {
    msgsEl.innerHTML = '';
    var d = document.createElement('div');
    d.className = 'wa-state';
    d.textContent = text;
    msgsEl.appendChild(d);
  }

  function showListSkeleton() {
    listEl.innerHTML = '';
    for (var i = 0; i < 8; i++) {
      var row = document.createElement('div');
      row.className = 'wa-skel';
      var circle = document.createElement('div');
      circle.className = 'wa-skel__circle';
      var lines = document.createElement('div');
      lines.className = 'wa-skel__lines';
      var l1 = document.createElement('div');
      l1.className = 'wa-skel__line';
      var l2 = document.createElement('div');
      l2.className = 'wa-skel__line wa-skel__line--short';
      lines.appendChild(l1);
      lines.appendChild(l2);
      row.appendChild(circle);
      row.appendChild(lines);
      listEl.appendChild(row);
    }
  }

  // ---------- chat model helpers ----------
  function chatId(c) { return wid(c && c.id); }

  function chatTitle(c) {
    return (c && (c.formattedTitle || c.name)) || (c && c.id && c.id.user) || 'Unknown chat';
  }

  function isJunkChat(c) {
    var id = chatId(c) || '';
    if (!id) { return true; }
    if (id.indexOf('@broadcast') !== -1) { return true; }
    try { if (c.archive) { return true; } } catch (e) {}
    try {
      if (c.id && (c.id.server === 'newsletter' || c.id.isNewsletter && c.id.isNewsletter())) {
        return true;
      }
    } catch (e) {}
    return false;
  }

  function lastTsMs(c) {
    var t = 0;
    try { t = (c.lastMessage && c.lastMessage.t) || c.t || 0; } catch (e) {}
    return t ? t * 1000 : 0;
  }

  function summaryLine(c) {
    var m = null;
    try { m = c.lastMessage; } catch (e) {}
    if (!m) { return 'Tap to start chatting'; }
    var prefix = m.fromMe ? 'You: ' : '';
    var label = mediaLabel(m.type);
    var text = (m.body && String(m.body).trim()) || '';
    if (label) { return prefix + label + (text ? ': ' + text : ''); }
    if (text) { return prefix + text; }
    return 'Open chat';
  }

  // ---------- chat list ----------
  function renderList() {
    var filtered = chats;
    if (searchTerm) {
      filtered = chats.filter(function (c) {
        return chatTitle(c).toLowerCase().indexOf(searchTerm) !== -1;
      });
    }
    listEl.innerHTML = '';
    if (!filtered.length) {
      showListState(searchTerm ? 'No chats match "' + searchTerm + '"' : 'No chats yet — messages will appear here.');
      return;
    }
    var limit = Math.min(filtered.length, 300);
    var frag = document.createDocumentFragment();
    for (var i = 0; i < limit; i++) {
      frag.appendChild(buildRow(filtered[i]));
    }
    if (filtered.length > limit) {
      var more = document.createElement('div');
      more.className = 'wa-state';
      more.textContent = 'Showing ' + limit + ' of ' + filtered.length + ' chats — use search to find more.';
      frag.appendChild(more);
    }
    listEl.appendChild(frag);
  }

  function buildRow(c) {
    var id = chatId(c);
    var title = chatTitle(c);
    var unread = 0;
    try { unread = c.unreadCount || 0; } catch (e) {}
    var ts = lastTsMs(c);

    var row = document.createElement('div');
    row.className = 'wa-chatrow';
    row.setAttribute('data-cid', id);

    var av = document.createElement('div');
    av.className = 'wa-avatar';
    av.style.background = avatarColor(title);
    var ini = document.createElement('span');
    ini.textContent = initials(title);
    av.appendChild(ini);

    var body = document.createElement('div');
    body.className = 'wa-chatrow__body';

    var top = document.createElement('div');
    top.className = 'wa-chatrow__top';
    var name = document.createElement('div');
    name.className = 'wa-chatrow__name';
    name.textContent = title;
    var right = document.createElement('div');
    right.className = 'wa-chatrow__right';
    var time = document.createElement('span');
    time.className = 'wa-chatrow__time' + (unread > 0 ? ' wa-chatrow__time--unread' : '');
    time.textContent = fmtListTime(ts);
    right.appendChild(time);
    if (unread > 0) {
      var badge = document.createElement('span');
      badge.className = 'wa-badge';
      badge.textContent = unread > 99 ? '99+' : String(unread);
      right.appendChild(badge);
    }
    top.appendChild(name);
    top.appendChild(right);

    var bottom = document.createElement('div');
    bottom.className = 'wa-chatrow__bottom';
    var preview = document.createElement('div');
    preview.className = 'wa-chatrow__preview';
    preview.textContent = summaryLine(c);
    bottom.appendChild(preview);

    body.appendChild(top);
    body.appendChild(bottom);
    row.appendChild(av);
    row.appendChild(body);

    row.addEventListener('click', function () {
      openChat(id, title, !!(c.isGroup || (c.id && c.id.isGroup && c.id.isGroup())));
    });
    return row;
  }

  function refreshChats() {
    if (!window.WPP || !WPP.chat || typeof WPP.chat.list !== 'function') {
      return Promise.resolve();
    }
    return WPP.chat.list().then(function (list) {
      chats = (list || []).filter(function (c) { return !isJunkChat(c); });
      chats.sort(function (a, b) { return lastTsMs(b) - lastTsMs(a); });
      chatById = Object.create(null);
      for (var i = 0; i < chats.length; i++) {
        chatById[chatId(chats[i])] = chats[i];
      }
      renderList();
    })['catch'](function (e) {
      showListState('Couldn\'t load chats: ' + (e && e.message ? e.message : 'unknown error'));
    });
  }

  function scheduleRefresh() {
    if (refreshScheduled) { return; }
    refreshScheduled = true;
    setTimeout(function () {
      refreshScheduled = false;
      refreshChats();
    }, 1200);
  }

  // ---------- conversation ----------
  function openChat(id, title, isGroup) {
    activeChat = { id: id, title: title, isGroup: !!isGroup };
    rendered = Object.create(null);
    bubbleById = Object.create(null);
    pendingSends = [];

    chatTitleEl.textContent = title;
    chatSubEl.textContent = isGroup ? 'Group' : (shortWid(id) || '');
    chatAvatarEl.textContent = initials(title);
    chatAvatarEl.style.background = avatarColor(title);
    inputEl.value = '';

    viewList.classList.remove('wa-view--active');
    viewChat.classList.add('wa-view--active');
    try { history.pushState({ waChat: 1 }, ''); } catch (e) {}

    loadMessages(id);

    var model = chatById[id];
    if (model && model.unreadCount > 0) {
      model.unreadCount = 0;
      renderList();
    }
    if (window.WPP && WPP.chat && WPP.chat.markIsRead) {
      WPP.chat.markIsRead(id)['catch'](function () {});
    }
  }

  function closeChat(fromPop) {
    if (!activeChat) { return; }
    activeChat = null;
    pendingSends = [];
    viewChat.classList.remove('wa-view--active');
    viewList.classList.add('wa-view--active');
    inputEl.value = '';
    showMsgsState('Select a chat');
    if (!fromPop) {
      try { history.back(); } catch (e) {}
    }
    scheduleRefresh();
  }

  window.addEventListener('popstate', function () {
    if (activeChat) { closeChat(true); }
  });

  function loadMessages(id) {
    msgsEl.innerHTML = '';
    var spin = document.createElement('div');
    spin.className = 'wa-spinner';
    msgsEl.appendChild(spin);

    WPP.chat.getMessages(id, { count: 40 }).then(function (msgs) {
      if (!activeChat || activeChat.id !== id) { return; }
      msgsEl.innerHTML = '';
      renderMessages(msgs || []);
      scrollBottom(true);
    })['catch'](function (e) {
      if (!activeChat || activeChat.id !== id) { return; }
      showMsgsState('Couldn\'t load messages: ' + (e && e.message ? e.message : 'unknown error'));
    });
  }

  function isSkippedMsg(m) {
    var t = m.type;
    if (SKIP_TYPES[t]) { return true; }
    try { if (m.isStatus) { return true; } } catch (e) {}
    var key = msgKey(m);
    if (key && key.indexOf('@broadcast') !== -1) { return true; }
    var label = mediaLabel(t);
    var text = (m.body && String(m.body).trim()) || '';
    if (!label && !text) { return true; }
    return false;
  }

  function renderMessages(list) {
    var sorted = list.slice().sort(function (a, b) { return ((a.t || 0) - (b.t || 0)); });
    var lastDay = '';
    for (var i = 0; i < sorted.length; i++) {
      var m = sorted[i];
      if (!m || isSkippedMsg(m)) { continue; }
      var key = msgKey(m) || ('gen_' + i + '_' + (m.t || 0));
      if (rendered[key]) { continue; }
      rendered[key] = true;

      var ms = (m.t || 0) * 1000;
      if (ms) {
        var dk = dayKey(ms);
        if (dk !== lastDay) {
          lastDay = dk;
          var div = document.createElement('div');
          div.className = 'wa-daydivider';
          div.textContent = fmtDayLabel(ms);
          msgsEl.appendChild(div);
        }
      }
      var el = buildBubble(m, { key: key, status: m.fromMe ? 'delivered' : null });
      bubbleById[key] = el;
      msgsEl.appendChild(el);
    }
  }

  function senderNameFor(m) {
    if (m.fromMe || !activeChat || !activeChat.isGroup) { return null; }
    try { if (m.notifyName) { return String(m.notifyName); } } catch (e) {}
    return shortWid(m.author || m.from);
  }

  function tickFor(status) {
    if (status === 'pending') { return { ch: '🕒', cls: '' }; }
    if (status === 'failed') { return { ch: '⚠', cls: 'wa-tick--failed' }; }
    if (status === 'read') { return { ch: '✓✓', cls: 'wa-tick--read' }; }
    if (status === 'delivered') { return { ch: '✓✓', cls: '' }; }
    return { ch: '✓', cls: '' };
  }

  function buildBubble(m, opts) {
    opts = opts || {};
    var wrap = document.createElement('div');
    wrap.className = 'wa-bubble ' + (m.fromMe ? 'wa-bubble--out' : 'wa-bubble--in');
    if (opts.key) { wrap.setAttribute('data-key', opts.key); }

    var senderName = opts.senderName !== undefined ? opts.senderName : senderNameFor(m);
    if (!m.fromMe && senderName) {
      var sender = document.createElement('div');
      sender.className = 'wa-bubble__sender';
      sender.textContent = senderName;
      sender.style.color = avatarColor(senderName);
      wrap.appendChild(sender);
    }

    var label = mediaLabel(m.type);
    if (label) {
      var lab = document.createElement('div');
      lab.className = 'wa-bubble__media';
      lab.textContent = label;
      wrap.appendChild(lab);
    }

    var text = (m.body && String(m.body)) || '';
    if (text) {
      var bodyEl = document.createElement('div');
      bodyEl.className = 'wa-bubble__text';
      bodyEl.textContent = text;
      wrap.appendChild(bodyEl);
    }

    var meta = document.createElement('div');
    meta.className = 'wa-bubble__meta';
    var time = document.createElement('span');
    time.className = 'wa-bubble__time';
    time.textContent = fmtClock((m.t || Date.now() / 1000) * 1000);
    meta.appendChild(time);

    if (m.fromMe) {
      var tick = document.createElement('span');
      var tk = tickFor(opts.status || 'delivered');
      tick.className = 'wa-tick' + (tk.cls ? ' ' + tk.cls : '');
      tick.textContent = tk.ch;
      meta.appendChild(tick);
    }
    wrap.appendChild(meta);
    return wrap;
  }

  function scrollBottom(force) {
    var near = msgsEl.scrollHeight - msgsEl.scrollTop - msgsEl.clientHeight < 140;
    if (force || near) { msgsEl.scrollTop = msgsEl.scrollHeight; }
  }

  // ---------- events from the engine ----------
  function onNewMessage(msg) {
    try {
      if (!msg || isSkippedMsg(msg)) { return; }

      // Adopt our own optimistic bubble if the engine echoed it back.
      if (msg.fromMe) {
        var body = (msg.body || '').trim();
        for (var i = pendingSends.length - 1; i >= 0; i--) {
          if (pendingSends[i].body === body) {
            var p = pendingSends[i];
            if (p.el && p.el.parentNode) { p.el.parentNode.removeChild(p.el); }
            delete rendered[p.key];
            delete bubbleById[p.key];
            pendingSends.splice(i, 1);
            break;
          }
        }
      }

      var cid = msgChatId(msg);
      var key = msgKey(msg);
      if (activeChat && cid === activeChat.id && key && !rendered[key]) {
        rendered[key] = true;
        var el = buildBubble(msg, { key: key, status: msg.fromMe ? 'delivered' : null });
        bubbleById[key] = el;
        msgsEl.appendChild(el);
        scrollBottom(false);
      }
      scheduleRefresh();
    } catch (e) {}
  }

  function onAckChange(ev) {
    try {
      var key = ev && ev.id ? (ev.id._serialized || ev.id.id) : null;
      var ack = ev ? ev.ack : null;
      if (!key || ack === null || ack === undefined) { return; }
      var el = bubbleById[key];
      if (!el) { return; }
      var tick = el.querySelector('.wa-tick');
      if (!tick) { return; }
      if (ack >= 3) {
        tick.textContent = '✓✓';
        tick.className = 'wa-tick wa-tick--read';
      } else if (ack === 2) {
        tick.textContent = '✓✓';
        tick.className = 'wa-tick';
      } else if (ack === 1) {
        tick.textContent = '✓';
        tick.className = 'wa-tick';
      } else if (ack === 0) {
        tick.textContent = '⚠';
        tick.className = 'wa-tick wa-tick--failed';
      }
    } catch (e) {}
  }

  function onRevoke(ev) {
    try {
      var key = ev && ev.id ? (ev.id._serialized || ev.id.id) : null;
      var el = key ? bubbleById[key] : null;
      if (!el) { return; }
      var textEl = el.querySelector('.wa-bubble__text');
      if (textEl) {
        textEl.textContent = '🚫 This message was deleted';
        textEl.className = 'wa-bubble__text wa-bubble__text--revoked';
      }
      var mediaEl = el.querySelector('.wa-bubble__media');
      if (mediaEl) { mediaEl.textContent = ''; }
      var tick = el.querySelector('.wa-tick');
      if (tick) { tick.textContent = ''; }
    } catch (e) {}
  }

  // ---------- sending ----------
  function send() {
    var text = (inputEl.value || '').replace(/\s+$/, '');
    if (!text.trim() || !activeChat) { return; }
    inputEl.value = '';

    var cid = activeChat.id;
    var tempId = 'temp_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7);
    var fake = { body: text, fromMe: true, t: Date.now() / 1000, type: 'chat' };
    var el = buildBubble(fake, { key: tempId, status: 'pending', senderName: null });
    rendered[tempId] = true;
    bubbleById[tempId] = el;
    pendingSends.push({ body: text.trim(), el: el, key: tempId });
    msgsEl.appendChild(el);
    scrollBottom(true);

    WPP.chat.sendTextMessage(cid, text).then(function (ret) {
      // retire the optimistic entry, keep the bubble, register the real key
      for (var i = pendingSends.length - 1; i >= 0; i--) {
        if (pendingSends[i].el === el) { pendingSends.splice(i, 1); }
      }
      var key = ret && ret.id ? (ret.id._serialized || ret.id.id) : null;
      if (key) {
        rendered[key] = true;
        bubbleById[key] = el;
      }
      var tick = el.querySelector('.wa-tick');
      if (tick) {
        tick.textContent = '✓';
        tick.className = 'wa-tick';
      }
    })['catch'](function (e) {
      var tick = el.querySelector('.wa-tick');
      if (tick) {
        tick.textContent = '⚠';
        tick.className = 'wa-tick wa-tick--failed';
      }
      toast('Message failed: ' + (e && e.message ? e.message : 'unknown error'));
    });
  }

  // ---------- header actions ----------
  function markActiveRead() {
    if (!activeChat) { return; }
    WPP.chat.markIsRead(activeChat.id).then(function () {
      var model = chatById[activeChat.id];
      if (model) { model.unreadCount = 0; renderList(); }
      toast('Marked as read');
    })['catch'](function (e) {
      toast('Could not mark as read: ' + (e && e.message ? e.message : ''));
    });
  }

  function bindEvents() {
    sendBtn.addEventListener('click', send);
    inputEl.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') { e.preventDefault(); send(); }
    });
    backBtn.addEventListener('click', function () { closeChat(false); });
    markReadBtn.addEventListener('click', markActiveRead);
    searchEl.addEventListener('input', function () {
      searchTerm = (searchEl.value || '').trim().toLowerCase();
      renderList();
    });

    logoutBtn.addEventListener('click', function () {
      if (!logoutArmed) {
        logoutArmed = true;
        logoutBtn.classList.add('wa-iconbtn--armed');
        toast('Tap again to log out of WhatsApp Web');
        logoutTimer = setTimeout(function () {
          logoutArmed = false;
          logoutBtn.classList.remove('wa-iconbtn--armed');
        }, 3200);
        return;
      }
      clearTimeout(logoutTimer);
      logoutArmed = false;
      logoutBtn.classList.remove('wa-iconbtn--armed');
      if (window.Android && Android.onLogOut) {
        Android.onLogOut();
      } else {
        toast('Log out unavailable');
      }
    });
  }

  // ---------- boot ----------
  function boot() {
    showListSkeleton();
    bindEvents();

    WPP.ev.on('chat.new_message', onNewMessage);
    WPP.ev.on('chat.msg_ack_change', onAckChange);
    WPP.ev.on('chat.msg_revoke', onRevoke);
    WPP.ev.on('chat.unread_count_changed', scheduleRefresh);
    WPP.ev.on('chat.new_chat', scheduleRefresh);

    refreshChats();
    setInterval(function () {
      if (!document.hidden) { refreshChats(); }
    }, 30000);
  }

  var waited = 0;
  (function waitForLib() {
    if (window.WPP && WPP.chat && typeof WPP.chat.list === 'function' && WPP.ev) {
      boot();
      return;
    }
    waited++;
    if (waited > 20) {
      showListState('The WhatsApp bridge library did not load. The official client remains available.');
      return;
    }
    setTimeout(waitForLib, 500);
  })();
})();
