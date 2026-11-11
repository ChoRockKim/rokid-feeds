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

// 로그인 화면: 폰 키보드는 안드로이드 기본 입력칸에만 글자를 보내고 웹 입력칸에는 못 넣는다(2026-11-11 사용자 확인).
// 그래서 앱의 기본 입력칸에서 받은 글자를 지금 고른 웹 입력칸에 넣는다. React 페이지라 value를 직접 바꾸면 무시되므로
// 입력칸 원래의 value setter로 넣고 input/change 이벤트를 보낸다.
(function () {
  if (window.__rokidSetValue) return;
  function target() {
    var el = document.querySelector('[data-rokid-focus]') || document.activeElement;
    return el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') ? el : null;
  }
  /** 고른 칸이 입력칸이면 {"type":"password","label":"비밀번호"}, 아니면 "". */
  window.__rokidFocusInfo = function () {
    var el = target();
    if (!el) return '';
    return JSON.stringify({ type: el.type || 'text', label: (el.getAttribute('aria-label') || el.placeholder || el.name || '').slice(0, 40) });
  };
  window.__rokidSetValue = function (text) {
    var el = target();
    if (!el) return false;
    var proto = el.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, text);
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    return true;
  };
})();
