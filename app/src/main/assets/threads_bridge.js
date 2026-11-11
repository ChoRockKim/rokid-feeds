// threads.com 페이지에 문서 시작 시 넣는다(ThreadsWeb.kt). 페이지가 받는 JSON(fetch·XHR)과 페이지에 박힌 JSON(script[data-sjs]) 중
// 글 데이터("taken_at")가 든 것만 앱(RokidThreads.onJson)으로 넘긴다. 페이지 동작은 바꾸지 않는다.
(function () {
  if (window.__rokidThreads) return;
  window.__rokidThreads = true;
  function send(text) {
    try {
      if (typeof text === 'string' && text.indexOf('"taken_at"') >= 0) RokidThreads.onJson(location.href, text);
    } catch (e) {}
  }
  var originalFetch = window.fetch;
  if (originalFetch) {
    window.fetch = function () {
      return originalFetch.apply(this, arguments).then(function (response) {
        try { response.clone().text().then(send, function () {}); } catch (e) {}
        return response;
      });
    };
  }
  var originalSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send = function () {
    this.addEventListener('load', function () {
      try { if (this.responseType === '' || this.responseType === 'text') send(this.responseText); } catch (e) {}
    });
    return originalSend.apply(this, arguments);
  };
  function scan() {
    var scripts = document.querySelectorAll('script[data-sjs]');
    for (var i = 0; i < scripts.length; i++) {
      if (!scripts[i].__rokidSeen) { scripts[i].__rokidSeen = true; send(scripts[i].textContent); }
    }
  }
  window.__rokidScan = scan;
  document.addEventListener('DOMContentLoaded', scan);
  window.addEventListener('load', scan);
})();

// 로그인 화면(웹을 띠 안에 보여 줄 때)용: 안경 쓸기로 입력칸·버튼 사이를 옮기고 탭으로 누른다. 글자는 폰 키보드가 포커스된 칸에 넣는다.
(function () {
  if (window.__rokidFocus) return;
  var SELECTOR = 'input:not([type=hidden]), textarea, button, a[href], [role=button], [role=link], [tabindex]:not([tabindex="-1"])';
  function visible(el) {
    var r = el.getBoundingClientRect();
    var s = getComputedStyle(el);
    return r.width > 4 && r.height > 4 && s.visibility !== 'hidden' && s.display !== 'none' && !el.disabled;
  }
  function mark(el) {
    var old = document.querySelector('[data-rokid-focus]');
    if (old) { old.style.outline = old.getAttribute('data-rokid-outline') || ''; old.removeAttribute('data-rokid-focus'); }
    el.setAttribute('data-rokid-outline', el.style.outline || '');
    el.setAttribute('data-rokid-focus', '1');
    el.style.outline = '3px solid #00ff66';
    el.style.outlineOffset = '2px';
  }
  window.__rokidFocus = function (delta) {
    var list = Array.prototype.filter.call(document.querySelectorAll(SELECTOR), visible);
    if (!list.length) return '';
    var current = document.querySelector('[data-rokid-focus]') || document.activeElement;
    var i = list.indexOf(current);
    var next = list[i < 0 ? (delta > 0 ? 0 : list.length - 1) : (i + delta + list.length) % list.length];
    next.focus({ preventScroll: true });
    next.scrollIntoView({ block: 'center' });
    mark(next);
    return (next.getAttribute('aria-label') || next.placeholder || next.innerText || next.tagName).slice(0, 40);
  };
  window.__rokidClick = function () {
    var el = document.querySelector('[data-rokid-focus]') || document.activeElement;
    if (!el || el === document.body) return false;
    el.focus();
    if (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA') el.click();
    return true;
  };
})();

// "앱에서 더 편리하게 이용해보세요 · 앱 다운로드" 같은 앱 설치 권유 띠가 작은 화면을 가린다(2026-11-11 확인, 높이의 약 40%).
// 화면에 고정된(fixed/sticky) 요소 중 권유 문구가 있고, 입력칸·로그인 버튼이 없고, 화면 60%보다 작은 것만 숨긴다.
// 로그인 길("Instagram으로 계속하기"가 든 창)은 건드리지 않는다.
(function () {
  if (window.__rokidHidePromos) return;
  var PROMO = /앱 다운로드|앱에서 더 편리하게|앱에서 더 많은 기능|앱에서 열기|앱 열기|앱 사용하기|Threads 열기|Get app|Get the app|better in the app|Download the app|Download Threads|Open app|Open in app|Use the app|Open Threads/i;
  var KEEP = /Instagram으로 계속|Continue with Instagram|로그인|Log in/i;
  function hide() {
    var limit = window.innerHeight * 0.6;
    var all = document.querySelectorAll('body *');
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      if (el.__rokidHidden) continue;
      var s = getComputedStyle(el);
      if (s.position !== 'fixed' && s.position !== 'sticky') continue;
      var text = el.innerText || '';
      if (!PROMO.test(text) || KEEP.test(text) || el.querySelector('input, textarea')) continue;
      if (el.getBoundingClientRect().height > limit) continue;
      el.__rokidHidden = true;
      el.style.setProperty('display', 'none', 'important');
    }
  }
  var pending = false;
  function schedule() {
    if (pending) return;
    pending = true;
    setTimeout(function () { pending = false; try { hide(); } catch (e) {} }, 300);
  }
  window.__rokidHidePromos = hide;
  function start() {
    schedule();
    new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();

// 폰 링크 로그인(rokid-conversate 서버의 /login 중계): 폰에서 받은 아이디·비밀번호·인증 코드를 threads.com 로그인 폼에 넣고 누른다.
// 로그인 페이지는 데스크톱 화면(아이디·비밀번호 칸이 바로 있음, 2026-11-11 확인). React 페이지라 value를 직접 바꾸면 무시되므로
// 입력칸 원래의 value setter로 넣고 input/change 이벤트를 보낸다.
(function () {
  if (window.__rokidLogin) return;
  function visible(el) {
    var r = el.getBoundingClientRect();
    var s = getComputedStyle(el);
    return r.width > 4 && r.height > 4 && s.visibility !== 'hidden' && s.display !== 'none' && !el.disabled;
  }
  function inputs() {
    return Array.prototype.filter.call(document.querySelectorAll('input:not([type=hidden]):not([type=submit]):not([type=checkbox])'), visible);
  }
  function setValue(el, text) {
    var proto = el.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, text);
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }
  function press(form, words) {
    var buttons = Array.prototype.filter.call(document.querySelectorAll('button, [type=submit], [role=button]'), visible);
    var b = buttons.find(function (x) { return words.test((x.innerText || x.value || '').trim()); });
    if (b) { b.click(); return true; }
    if (form && form.requestSubmit) { form.requestSubmit(); return true; }
    return false;
  }
  function label(el) { return (el.getAttribute('aria-label') || el.placeholder || el.name || '').slice(0, 60); }
  window.__rokidLogin = function (user, pass) {
    var list = inputs();
    var p = list.find(function (x) { return x.type === 'password'; });
    var u = list.find(function (x) { return x.autocomplete === 'username' || x.name === 'username'; }) ||
      list.find(function (x) { return x !== p && (x.type === 'text' || x.type === 'email' || x.type === 'tel'); });
    if (!u || !p) return JSON.stringify({ ok: false, why: 'no-form' });
    setValue(u, user);
    setValue(p, pass);
    setTimeout(function () { press(p.form, /^(로그인|Log in|Log In)$/); }, 300);
    return JSON.stringify({ ok: true });
  };
  /** 로그인 뒤 페이지 상태: 인증 코드 칸이 있는지, 오류 문구가 있는지. */
  window.__rokidLoginState = function () {
    var list = inputs();
    var loginForm = list.some(function (x) { return x.type === 'password'; });
    var code = loginForm ? null : list.find(function (x) { return x.type !== 'password'; });
    var alert = document.querySelector('[role=alert]');
    var error = alert && alert.innerText.trim();
    if (!error) {
      var m = (document.body.innerText || '').match(/[^\n]*(잘못|올바르지|다시 시도|incorrect|wrong|try again|문제가 발생)[^\n]*/i);
      error = m ? m[0].trim() : '';
    }
    return JSON.stringify({ loginForm: loginForm, code: code ? (label(code) || '인증 코드') : null, error: (error || '').slice(0, 120) });
  };
  window.__rokidFillCode = function (text) {
    var list = inputs().filter(function (x) { return x.type !== 'password'; });
    var el = list.find(function (x) { return x.autocomplete === 'one-time-code' || /code|코드/i.test(x.name + label(x)); }) || list[0];
    if (!el) return false;
    setValue(el, text);
    setTimeout(function () { press(el.form, /^(확인|Confirm|다음|Next|제출|Submit|계속|Continue)$/); }, 300);
    return true;
  };
})();

// 목록 더 불러오기: threads.com은 창이 아니라 안쪽 상자가 스크롤되기도 한다. 마지막 글을 화면에 올리고,
// 스크롤되는 상자와 창을 모두 끝까지 내린다. 돌려주는 값은 지금 페이지의 글 링크 수(디버그용).
window.__rokidLoadMore = function () {
  var links = document.querySelectorAll('a[href*="/post/"]');
  var last = links[links.length - 1];
  if (last && last.scrollIntoView) last.scrollIntoView({ block: 'end' });
  var boxes = [document.scrollingElement];
  var divs = document.querySelectorAll('div');
  for (var i = 0; i < divs.length; i++) {
    var d = divs[i];
    if (d.scrollHeight > d.clientHeight + 20) {
      var o = getComputedStyle(d).overflowY;
      if (o === 'auto' || o === 'scroll') boxes.push(d);
    }
  }
  boxes.forEach(function (b) { if (b) b.scrollTop = b.scrollHeight; });
  window.scrollTo(0, document.scrollingElement.scrollHeight);
  window.dispatchEvent(new Event('scroll'));
  return links.length;
};
