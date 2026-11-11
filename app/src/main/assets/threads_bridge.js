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
