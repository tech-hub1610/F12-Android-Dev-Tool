(function() {
  if (window.__F12_INITIALIZED__) return;
  window.__F12_INITIALIZED__ = true;

  var nativeBridge = window.DevToolsNative || null;
  var domNodeIdCounter = 1;
  var inspectedElement = null;

  function safeStringify(obj, maxLen) {
    try {
      var seen = [];
      var str = JSON.stringify(obj, function(key, val) {
        if (typeof val === 'object' && val !== null) {
          if (seen.indexOf(val) !== -1) return '[Circular]';
          seen.push(val);
        }
        return val;
      });
      if (maxLen && str && str.length > maxLen) {
        return str.substring(0, maxLen) + '... (truncated)';
      }
      return str || '';
    } catch (e) {
      return String(obj);
    }
  }

  function sendToNative(handler, data) {
    if (nativeBridge && typeof nativeBridge[handler] === 'function') {
      try {
        nativeBridge[handler](typeof data === 'string' ? data : JSON.stringify(data));
      } catch (err) {}
    }
  }

  // ==========================================
  // 1. Console Hooking
  // ==========================================
  var origConsole = {
    log: console.log,
    info: console.info,
    warn: console.warn,
    error: console.error,
    debug: console.debug,
    clear: console.clear
  };

  function hookConsoleMethod(level) {
    var orig = origConsole[level] || console.log;
    console[level] = function() {
      var args = Array.prototype.slice.call(arguments);
      try {
        orig.apply(console, args);
      } catch (e) {}

      var formattedArgs = args.map(function(arg) {
        if (typeof arg === 'undefined') return 'undefined';
        if (arg === null) return 'null';
        if (typeof arg === 'object') {
          if (arg instanceof Error) {
            return arg.name + ': ' + arg.message + '\n' + (arg.stack || '');
          }
          return safeStringify(arg, 2048);
        }
        return String(arg);
      });

      sendToNative('onConsoleMessage', {
        level: level,
        message: formattedArgs.join(' '),
        timestamp: Date.now()
      });
    };
  }

  hookConsoleMethod('log');
  hookConsoleMethod('info');
  hookConsoleMethod('warn');
  hookConsoleMethod('error');
  hookConsoleMethod('debug');

  console.clear = function() {
    try { origConsole.clear.apply(console); } catch (e) {}
    sendToNative('onConsoleClear', {});
  };

  window.addEventListener('error', function(event) {
    sendToNative('onConsoleMessage', {
      level: 'error',
      message: (event.message || 'Uncaught Error') + (event.filename ? (' at ' + event.filename + ':' + event.lineno + ':' + event.colno) : ''),
      source: event.filename || '',
      lineno: event.lineno || 0,
      colno: event.colno || 0,
      timestamp: Date.now()
    });
  });

  window.addEventListener('unhandledrejection', function(event) {
    var reason = event.reason;
    var msg = 'Unhandled Promise Rejection: ' + (reason ? (reason.message || String(reason)) : 'unknown');
    sendToNative('onConsoleMessage', {
      level: 'error',
      message: msg,
      timestamp: Date.now()
    });
  });

  // ==========================================
  // 2. Network Hooking (Fetch & XHR)
  // ==========================================
  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function() {
      var args = Array.prototype.slice.call(arguments);
      var url = '';
      var options = args[1] || {};
      var method = (options.method || 'GET').toUpperCase();
      var startTime = Date.now();

      if (typeof args[0] === 'string') {
        url = args[0];
      } else if (args[0] && args[0].url) {
        url = args[0].url;
        if (!options.method && args[0].method) {
          method = args[0].method.toUpperCase();
        }
      }

      var reqHeaders = {};
      if (options.headers) {
        try {
          if (typeof options.headers.forEach === 'function') {
            options.headers.forEach(function(v, k) { reqHeaders[k] = v; });
          } else {
            reqHeaders = options.headers;
          }
        } catch (e) {}
      }

      var reqBody = '';
      if (options.body) {
        if (typeof options.body === 'string') reqBody = options.body.substring(0, 5000);
        else reqBody = '[Body Object]';
      }

      return origFetch.apply(window, args).then(function(response) {
        var duration = Date.now() - startTime;
        var clone = response.clone();
        var respHeaders = {};
        try {
          if (clone.headers && typeof clone.headers.forEach === 'function') {
            clone.headers.forEach(function(v, k) { respHeaders[k] = v; });
          }
        } catch (e) {}

        clone.text().then(function(text) {
          sendToNative('onNetworkRequest', {
            url: url,
            method: method,
            status: response.status,
            statusText: response.statusText,
            duration: duration,
            startTime: startTime,
            type: 'fetch',
            requestHeaders: reqHeaders,
            requestBody: reqBody,
            responseHeaders: respHeaders,
            responseBody: (text || '').substring(0, 30000)
          });
        }).catch(function() {
          sendToNative('onNetworkRequest', {
            url: url,
            method: method,
            status: response.status,
            statusText: response.statusText,
            duration: duration,
            startTime: startTime,
            type: 'fetch',
            requestHeaders: reqHeaders,
            requestBody: reqBody,
            responseHeaders: respHeaders,
            responseBody: '[Response body read error]'
          });
        });

        return response;
      }).catch(function(err) {
        var duration = Date.now() - startTime;
        sendToNative('onNetworkRequest', {
          url: url,
          method: method,
          status: 0,
          statusText: err.message || 'Failed',
          duration: duration,
          startTime: startTime,
          type: 'fetch',
          requestHeaders: reqHeaders,
          requestBody: reqBody,
          responseHeaders: {},
          responseBody: 'Error: ' + err.message
        });
        throw err;
      });
    };
  }

  var origXHR = window.XMLHttpRequest;
  if (origXHR) {
    var XHRProto = origXHR.prototype;
    var origOpen = XHRProto.open;
    var origSend = XHRProto.send;
    var origSetRequestHeader = XHRProto.setRequestHeader;

    XHRProto.open = function(method, url) {
      this._f12_method = (method || 'GET').toUpperCase();
      this._f12_url = url;
      this._f12_reqHeaders = {};
      return origOpen.apply(this, arguments);
    };

    XHRProto.setRequestHeader = function(header, value) {
      if (this._f12_reqHeaders) {
        this._f12_reqHeaders[header] = value;
      }
      return origSetRequestHeader.apply(this, arguments);
    };

    XHRProto.send = function(body) {
      var self = this;
      var startTime = Date.now();
      var reqBody = '';
      if (body) {
        if (typeof body === 'string') reqBody = body.substring(0, 5000);
        else reqBody = '[XHR Body]';
      }

      this.addEventListener('loadend', function() {
        var duration = Date.now() - startTime;
        var respHeaders = {};
        try {
          var rawH = self.getAllResponseHeaders() || '';
          var lines = rawH.trim().split(/[\r\n]+/);
          lines.forEach(function(line) {
            var parts = line.split(': ');
            var h = parts.shift();
            var val = parts.join(': ');
            if (h) respHeaders[h] = val;
          });
        } catch (e) {}

        var respText = '';
        try {
          respText = (self.responseText || '').substring(0, 30000);
        } catch (e) {
          respText = '[Binary/Unavailable]';
        }

        sendToNative('onNetworkRequest', {
          url: self._f12_url || '',
          method: self._f12_method || 'GET',
          status: self.status,
          statusText: self.statusText || '',
          duration: duration,
          startTime: startTime,
          type: 'xhr',
          requestHeaders: self._f12_reqHeaders || {},
          requestBody: reqBody,
          responseHeaders: respHeaders,
          responseBody: respText
        });
      });

      return origSend.apply(this, arguments);
    };
  }

  // ==========================================
  // 3. Highlight Overlay & Touch Inspector
  // ==========================================
  var overlayEl = null;
  var tooltipEl = null;
  var isInspectorActive = false;

  function ensureOverlay() {
    if (!overlayEl) {
      overlayEl = document.createElement('div');
      overlayEl.id = 'f12-inspect-overlay';
      overlayEl.style.position = 'fixed';
      overlayEl.style.pointerEvents = 'none';
      overlayEl.style.zIndex = '2147483647';
      overlayEl.style.background = 'rgba(59, 130, 246, 0.25)';
      overlayEl.style.border = '2px solid #2563EB';
      overlayEl.style.display = 'none';
      overlayEl.style.transition = 'all 0.1s ease-out';

      tooltipEl = document.createElement('div');
      tooltipEl.style.position = 'absolute';
      tooltipEl.style.top = '-26px';
      tooltipEl.style.left = '0';
      tooltipEl.style.background = '#1E293B';
      tooltipEl.style.color = '#FFFFFF';
      tooltipEl.style.fontFamily = 'monospace';
      tooltipEl.style.fontSize = '11px';
      tooltipEl.style.padding = '2px 6px';
      tooltipEl.style.border = '1px solid #3B82F6';
      tooltipEl.style.borderRadius = '4px';
      tooltipEl.style.whiteSpace = 'nowrap';
      overlayEl.appendChild(tooltipEl);

      document.documentElement.appendChild(overlayEl);
    }
  }

  function getOrAssignDomId(el) {
    if (!el || el.nodeType !== 1) return null;
    var id = el.getAttribute('data-f12-id');
    if (!id) {
      id = 'f12_node_' + (domNodeIdCounter++);
      el.setAttribute('data-f12-id', id);
    }
    return id;
  }

  function findElementByDomId(id) {
    if (!id) return null;
    return document.querySelector('[data-f12-id="' + id + '"]');
  }

  // ==========================================
  // Public F12 API
  // ==========================================
  window.__F12__ = {
    getDomTree: function(maxDepth) {
      maxDepth = maxDepth || 5;

      function serializeNode(node, depth) {
        if (!node) return null;
        if (node.nodeType === 3) {
          // Text node
          var text = node.textContent.trim();
          if (!text) return null;
          return {
            nodeType: 3,
            text: text.length > 60 ? text.substring(0, 60) + '...' : text
          };
        }
        if (node.nodeType !== 1) return null;
        if (node.id === 'f12-inspect-overlay') return null;

        var domId = getOrAssignDomId(node);
        var attrs = {};
        for (var i = 0; i < node.attributes.length; i++) {
          var a = node.attributes[i];
          if (a.name !== 'data-f12-id') {
            attrs[a.name] = a.value;
          }
        }

        var childNodes = [];
        if (depth < maxDepth) {
          for (var j = 0; j < node.childNodes.length; j++) {
            var c = serializeNode(node.childNodes[j], depth + 1);
            if (c) childNodes.push(c);
          }
        }

        return {
          nodeType: 1,
          f12Id: domId,
          tagName: node.tagName.toLowerCase(),
          id: node.id || '',
          className: node.className || '',
          attributes: attrs,
          childCount: node.children ? node.children.length : 0,
          children: childNodes
        };
      }

      return JSON.stringify(serializeNode(document.documentElement, 0));
    },

    highlightElement: function(domId) {
      ensureOverlay();
      var el = findElementByDomId(domId);
      if (!el) {
        overlayEl.style.display = 'none';
        return;
      }
      inspectedElement = el;
      window.$0 = el;

      var rect = el.getBoundingClientRect();
      overlayEl.style.top = rect.top + 'px';
      overlayEl.style.left = rect.left + 'px';
      overlayEl.style.width = Math.max(rect.width, 2) + 'px';
      overlayEl.style.height = Math.max(rect.height, 2) + 'px';
      overlayEl.style.display = 'block';

      var desc = el.tagName.toLowerCase();
      if (el.id) desc += '#' + el.id;
      else if (el.className && typeof el.className === 'string') {
        desc += '.' + el.className.split(' ').filter(Boolean).join('.');
      }
      desc += ' | ' + Math.round(rect.width) + 'x' + Math.round(rect.height);
      tooltipEl.textContent = desc;

      try {
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
      } catch (e) {}
    },

    clearHighlight: function() {
      if (overlayEl) overlayEl.style.display = 'none';
    },

    getElementDetails: function(domId) {
      var el = findElementByDomId(domId);
      if (!el) return null;

      var attrs = {};
      for (var i = 0; i < el.attributes.length; i++) {
        var a = el.attributes[i];
        if (a.name !== 'data-f12-id') {
          attrs[a.name] = a.value;
        }
      }

      var computed = window.getComputedStyle(el);
      var boxModel = {
        margin: {
          top: computed.marginTop,
          right: computed.marginRight,
          bottom: computed.marginBottom,
          left: computed.marginLeft
        },
        border: {
          top: computed.borderTopWidth,
          right: computed.borderRightWidth,
          bottom: computed.borderBottomWidth,
          left: computed.borderLeftWidth
        },
        padding: {
          top: computed.paddingTop,
          right: computed.paddingRight,
          bottom: computed.paddingBottom,
          left: computed.paddingLeft
        },
        content: {
          width: Math.round(el.getBoundingClientRect().width) + 'px',
          height: Math.round(el.getBoundingClientRect().height) + 'px'
        }
      };

      var styles = {
        display: computed.display,
        position: computed.position,
        color: computed.color,
        backgroundColor: computed.backgroundColor,
        fontSize: computed.fontSize,
        fontFamily: computed.fontFamily,
        fontWeight: computed.fontWeight,
        lineHeight: computed.lineHeight,
        textAlign: computed.textAlign,
        width: computed.width,
        height: computed.height,
        opacity: computed.opacity,
        zIndex: computed.zIndex,
        boxSizing: computed.boxSizing,
        overflow: computed.overflow
      };

      return JSON.stringify({
        f12Id: domId,
        tagName: el.tagName.toLowerCase(),
        id: el.id || '',
        className: el.className || '',
        outerHTML: el.outerHTML ? el.outerHTML.replace(/\sdata-f12-id="[^"]*"/, '') : '',
        attributes: attrs,
        styles: styles,
        boxModel: boxModel
      });
    },

    updateElementHTML: function(domId, newHTML) {
      var el = findElementByDomId(domId);
      if (!el) return false;
      try {
        el.outerHTML = newHTML;
        return true;
      } catch (e) {
        return false;
      }
    },

    updateElementStyle: function(domId, prop, val) {
      var el = findElementByDomId(domId);
      if (!el) return false;
      try {
        el.style[prop] = val;
        return true;
      } catch (e) {
        return false;
      }
    },

    removeElement: function(domId) {
      var el = findElementByDomId(domId);
      if (!el) return false;
      if (el.parentNode) {
        el.parentNode.removeChild(el);
        this.clearHighlight();
        return true;
      }
      return false;
    },

    setInspectorActive: function(active) {
      isInspectorActive = !!active;
      if (isInspectorActive) {
        ensureOverlay();
        document.addEventListener('click', this._onInspectClick, true);
        document.addEventListener('touchstart', this._onInspectTouch, { passive: false, capture: true });
      } else {
        document.removeEventListener('click', this._onInspectClick, true);
        document.removeEventListener('touchstart', this._onInspectTouch, { passive: false, capture: true });
        this.clearHighlight();
      }
    },

    _onInspectClick: function(e) {
      if (!isInspectorActive) return;
      e.preventDefault();
      e.stopPropagation();
      var target = e.target;
      if (target === overlayEl || target === tooltipEl) return;
      var domId = getOrAssignDomId(target);
      window.__F12__.highlightElement(domId);
      sendToNative('onElementSelected', { f12Id: domId });
    },

    _onInspectTouch: function(e) {
      if (!isInspectorActive) return;
      e.preventDefault();
      e.stopPropagation();
      if (e.touches && e.touches[0]) {
        var touch = e.touches[0];
        var target = document.elementFromPoint(touch.clientX, touch.clientY);
        if (target && target !== overlayEl && target !== tooltipEl) {
          var domId = getOrAssignDomId(target);
          window.__F12__.highlightElement(domId);
          sendToNative('onElementSelected', { f12Id: domId });
        }
      }
    },

    // ==========================================
    // Performance & Storage
    // ==========================================
    getPerformanceMetrics: function() {
      var nav = {};
      if (window.performance && window.performance.timing) {
        var t = window.performance.timing;
        var start = t.navigationStart || t.fetchStart;
        nav = {
          dns: (t.domainLookupEnd - t.domainLookupStart) || 0,
          tcp: (t.connectEnd - t.connectStart) || 0,
          ttfb: (t.responseStart - t.requestStart) || 0,
          domContentLoaded: (t.domContentLoadedEventEnd - start) || 0,
          loadComplete: (t.loadEventEnd - start) || 0
        };
      }

      var paints = {};
      if (window.performance && typeof window.performance.getEntriesByType === 'function') {
        var paintEntries = window.performance.getEntriesByType('paint');
        for (var i = 0; i < paintEntries.length; i++) {
          paints[paintEntries[i].name] = Math.round(paintEntries[i].startTime);
        }
      }

      var memory = null;
      if (window.performance && window.performance.memory) {
        var m = window.performance.memory;
        memory = {
          totalJSHeap: Math.round(m.totalJSHeapSize / (1024 * 1024) * 10) / 10,
          usedJSHeap: Math.round(m.usedJSHeapSize / (1024 * 1024) * 10) / 10,
          jsHeapLimit: Math.round(m.jsHeapSizeLimit / (1024 * 1024) * 10) / 10
        };
      }

      var domCount = document.getElementsByTagName('*').length;
      var scriptsCount = document.getElementsByTagName('script').length;
      var stylesCount = document.getElementsByTagName('link').length + document.getElementsByTagName('style').length;
      var imagesCount = document.getElementsByTagName('img').length;

      return JSON.stringify({
        navigation: nav,
        paints: paints,
        memory: memory,
        domCount: domCount,
        scriptsCount: scriptsCount,
        stylesCount: stylesCount,
        imagesCount: imagesCount
      });
    },

    getStorageData: function() {
      var local = {};
      try {
        for (var i = 0; i < localStorage.length; i++) {
          var k = localStorage.key(i);
          local[k] = localStorage.getItem(k);
        }
      } catch (e) {}

      var session = {};
      try {
        for (var j = 0; j < sessionStorage.length; j++) {
          var sk = sessionStorage.key(j);
          session[sk] = sessionStorage.getItem(sk);
        }
      } catch (e) {}

      var cookies = [];
      try {
        var cStr = document.cookie || '';
        if (cStr) {
          cookies = cStr.split(';').map(function(c) {
            var parts = c.trim().split('=');
            return { name: parts[0], value: parts.slice(1).join('=') };
          });
        }
      } catch (e) {}

      return JSON.stringify({
        localStorage: local,
        sessionStorage: session,
        cookies: cookies
      });
    },

    setStorageItem: function(type, key, value) {
      try {
        if (type === 'local') localStorage.setItem(key, value);
        else if (type === 'session') sessionStorage.setItem(key, value);
        return true;
      } catch (e) {
        return false;
      }
    },

    removeStorageItem: function(type, key) {
      try {
        if (type === 'local') localStorage.removeItem(key);
        else if (type === 'session') sessionStorage.removeItem(key);
        return true;
      } catch (e) {
        return false;
      }
    },

    clearStorage: function(type) {
      try {
        if (type === 'local') localStorage.clear();
        else if (type === 'session') sessionStorage.clear();
        return true;
      } catch (e) {
        return false;
      }
    },

    // ==========================================
    // Responsive Simulation
    // ==========================================
    setViewportMeta: function(width, height) {
      try {
        var meta = document.querySelector('meta[name="viewport"]');
        if (!meta) {
          meta = document.createElement('meta');
          meta.name = 'viewport';
          document.head.appendChild(meta);
        }
        if (width > 0) {
          meta.content = 'width=' + width + ', initial-scale=1.0';
        } else {
          meta.content = 'width=device-width, initial-scale=1.0';
        }
        return true;
      } catch (e) {
        return false;
      }
    },

    // ==========================================
    // Sources Listing
    // ==========================================
    getPageSources: function() {
      var sources = [];
      sources.push({
        type: 'html',
        url: window.location.href,
        title: 'Document (HTML)',
        content: document.documentElement.outerHTML.substring(0, 100000)
      });

      var scripts = document.getElementsByTagName('script');
      for (var i = 0; i < scripts.length; i++) {
        var s = scripts[i];
        if (s.src) {
          sources.push({
            type: 'script',
            url: s.src,
            title: s.src.split('/').pop() || s.src,
            content: ''
          });
        } else if (s.textContent.trim()) {
          sources.push({
            type: 'inline-script',
            url: 'inline-script-' + (i + 1),
            title: 'Inline Script #' + (i + 1),
            content: s.textContent.trim().substring(0, 50000)
          });
        }
      }

      var links = document.getElementsByTagName('link');
      for (var j = 0; j < links.length; j++) {
        var l = links[j];
        if (l.rel === 'stylesheet' && l.href) {
          sources.push({
            type: 'stylesheet',
            url: l.href,
            title: l.href.split('/').pop() || l.href,
            content: ''
          });
        }
      }

      return JSON.stringify(sources);
    },

    // ==========================================
    // Lighthouse Audit Engine
    // ==========================================
    runAudits: function() {
      var audits = [];

      // 1. SEO
      var title = document.title ? document.title.trim() : '';
      audits.push({
        category: 'SEO',
        name: 'Document has a <title> element',
        score: title.length > 0 ? 1 : 0,
        severity: title.length > 0 ? 'PASS' : 'FAIL',
        evidence: title ? ('Title: "' + title + '"') : 'Missing title element',
        fix: 'Add a descriptive <title> to the <head> element.'
      });

      var metaDesc = document.querySelector('meta[name="description"]');
      var descContent = metaDesc ? metaDesc.getAttribute('content') : '';
      audits.push({
        category: 'SEO',
        name: 'Document has meta description',
        score: descContent ? 1 : 0,
        severity: descContent ? 'PASS' : 'WARN',
        evidence: descContent ? ('Description: ' + descContent.substring(0, 50) + '...') : 'No meta description found',
        fix: 'Add <meta name="description" content="..."> for search engine snippets.'
      });

      var h1s = document.getElementsByTagName('h1');
      audits.push({
        category: 'SEO',
        name: 'Structured heading hierarchy (h1)',
        score: h1s.length === 1 ? 1 : (h1s.length > 1 ? 0.7 : 0),
        severity: h1s.length === 1 ? 'PASS' : (h1s.length > 1 ? 'WARN' : 'FAIL'),
        evidence: 'Found ' + h1s.length + ' <h1> element(s)',
        fix: 'Use exactly one primary <h1> per page for optimal search ranking.'
      });

      // 2. Accessibility
      var images = document.getElementsByTagName('img');
      var missingAlt = 0;
      for (var i = 0; i < images.length; i++) {
        if (!images[i].hasAttribute('alt')) missingAlt++;
      }
      audits.push({
        category: 'Accessibility',
        name: 'Image elements have alt attributes',
        score: images.length === 0 ? 1 : Math.max(0, (images.length - missingAlt) / images.length),
        severity: missingAlt === 0 ? 'PASS' : (missingAlt < images.length / 2 ? 'WARN' : 'FAIL'),
        evidence: missingAlt + ' of ' + images.length + ' images are missing alt text',
        fix: 'Add alt attributes to all <img> tags to assist screen reader users.'
      });

      var htmlLang = document.documentElement.getAttribute('lang');
      audits.push({
        category: 'Accessibility',
        name: '<html> element has a [lang] attribute',
        score: htmlLang ? 1 : 0,
        severity: htmlLang ? 'PASS' : 'FAIL',
        evidence: htmlLang ? ('lang="' + htmlLang + '"') : 'Missing lang attribute on <html>',
        fix: 'Add lang="en" (or appropriate language code) to the <html> tag.'
      });

      var inputs = document.querySelectorAll('input:not([type="hidden"]), select, textarea');
      var missingLabel = 0;
      for (var j = 0; j < inputs.length; j++) {
        var inp = inputs[j];
        var id = inp.id;
        var hasLabel = false;
        if (id && document.querySelector('label[for="' + id + '"]')) hasLabel = true;
        if (inp.closest('label')) hasLabel = true;
        if (inp.getAttribute('aria-label') || inp.getAttribute('aria-labelledby')) hasLabel = true;
        if (!hasLabel) missingLabel++;
      }
      audits.push({
        category: 'Accessibility',
        name: 'Form elements have associated labels',
        score: inputs.length === 0 ? 1 : Math.max(0, (inputs.length - missingLabel) / inputs.length),
        severity: missingLabel === 0 ? 'PASS' : 'WARN',
        evidence: missingLabel + ' form control(s) missing accessible label',
        fix: 'Associate <label for="id"> or aria-label with every input control.'
      });

      // 3. Best Practices
      var isHttps = window.location.protocol === 'https:';
      audits.push({
        category: 'Best Practices',
        name: 'Uses secure HTTPS connection',
        score: isHttps ? 1 : 0,
        severity: isHttps ? 'PASS' : 'FAIL',
        evidence: 'Protocol is ' + window.location.protocol,
        fix: 'Serve page and all assets over HTTPS to ensure confidentiality and integrity.'
      });

      var doctype = document.doctype;
      audits.push({
        category: 'Best Practices',
        name: 'Page has valid HTML5 Doctype',
        score: doctype && doctype.name.toLowerCase() === 'html' ? 1 : 0,
        severity: doctype ? 'PASS' : 'WARN',
        evidence: doctype ? '<!DOCTYPE ' + doctype.name + '>' : 'No DOCTYPE declared',
        fix: 'Ensure <!DOCTYPE html> appears at the very beginning of the document.'
      });

      var charsetMeta = document.querySelector('meta[charset]') || document.querySelector('meta[http-equiv="Content-Type"]');
      audits.push({
        category: 'Best Practices',
        name: 'Character encoding is declared',
        score: charsetMeta ? 1 : 0,
        severity: charsetMeta ? 'PASS' : 'WARN',
        evidence: charsetMeta ? 'Charset declared' : 'No charset declaration found',
        fix: 'Include <meta charset="UTF-8"> early in the <head>.'
      });

      // 4. Performance
      var domElements = document.getElementsByTagName('*').length;
      audits.push({
        category: 'Performance',
        name: 'Avoid excessive DOM size',
        score: domElements < 800 ? 1 : (domElements < 1500 ? 0.7 : 0.3),
        severity: domElements < 800 ? 'PASS' : (domElements < 1500 ? 'WARN' : 'FAIL'),
        evidence: 'Total DOM elements: ' + domElements + ' (recommended < 800)',
        fix: 'Simplify page hierarchy and virtualize large lists to reduce DOM nodes.'
      });

      // 5. Mobile Usability
      var viewportMeta = document.querySelector('meta[name="viewport"]');
      var hasMobileViewport = viewportMeta && viewportMeta.getAttribute('content') && viewportMeta.getAttribute('content').indexOf('width=device-width') !== -1;
      audits.push({
        category: 'Mobile Usability',
        name: 'Has <meta name="viewport"> tag with width=device-width',
        score: hasMobileViewport ? 1 : 0,
        severity: hasMobileViewport ? 'PASS' : 'FAIL',
        evidence: viewportMeta ? viewportMeta.getAttribute('content') : 'No viewport meta tag found',
        fix: 'Add <meta name="viewport" content="width=device-width, initial-scale=1.0"> for mobile screens.'
      });

      return JSON.stringify(audits);
    }
  };

  sendToNative('onBridgeReady', { initialized: true, url: window.location.href });
})();
