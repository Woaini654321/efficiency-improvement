/**
 * DataTracker — 用户浏览数据埋点
 *
 * 使用方式：在所有 HTML 页面中 <script src="data-tracker.js"></script>
 * 然后在 React 组件挂载时调用：window.DataTracker.trackPageView('opp-hall', '首页');
 */

(function() {
  'use strict';

  var STORAGE_KEY = 'analytics_events';
  var MAX_EVENTS = 5000;
  var DEDUP_WINDOW_MS = 30000; // 30秒内同页面不重复记录
  var SESSION_START = Date.now();
  var lastPageView = {}; // { pageKey: timestamp }

  /* ========== 工具函数 ========== */
  function generateId() {
    var now = new Date();
    var ts = now.toISOString().replace(/[-:]/g,'').slice(0,15);
    var rand = Math.random().toString(36).slice(2, 8);
    return 'evt_' + ts + '_' + rand;
  }

  function getEvents() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    } catch(e) { return []; }
  }

  function saveEvents(events) {
    // 容量管理：最多保留 MAX_EVENTS 条
    if (events.length > MAX_EVENTS) {
      events = events.slice(events.length - MAX_EVENTS);
    }
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
    } catch(e) {
      // localStorage 满时移除一半旧事件
      if (e.name === 'QuotaExceededError') {
        events = events.slice(Math.floor(events.length / 2));
        localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
      }
    }
  }

  function pushEvent(event) {
    var events = getEvents();
    events.push(event);
    saveEvents(events);
  }

  function getUserRole() {
    // 尝试从 shell 或 localStorage 获取Current.User 当前用户角色
    if (window.PROTO_SHELL && window.PROTO_SHELL.currentUser) {
      return window.PROTO_SHELL.currentUser.role || 'unknown';
    }
    // 管理端页面检测
    var pageName = window.location.pathname.split('/').pop() || '';
    if (pageName.indexOf('PC-09') >= 0 || pageName.indexOf('PC-10') >= 0 ||
        pageName.indexOf('PC-11') >= 0 || pageName.indexOf('PC-12') >= 0 ||
        pageName.indexOf('PC-13') >= 0 || pageName.indexOf('PC-14') >= 0) {
      return 'admin';
    }
    return 'unknown';
  }

  function getCurrentPageFileName() {
    var path = window.location.pathname;
    return path.split('/').pop() || 'index.html';
  }

  /* ========== 核心 API ========== */

  /**
   * trackPageView — 记录页面浏览
   * @param {string} pageKey - 页面标识，如 'opp-hall', 'req-square'
   * @param {string} pageTitle - 页面中文名，如 '首页'
   */
  function trackPageView(pageKey, pageTitle) {
    // 去重：同页面30秒内不重复记录
    var now = Date.now();
    if (lastPageView[pageKey] && (now - lastPageView[pageKey]) < DEDUP_WINDOW_MS) {
      return;
    }
    lastPageView[pageKey] = now;

    pushEvent({
      event_id: generateId(),
      event_type: 'page_view',
      page: pageKey,
      page_title: pageTitle || '',
      url: getCurrentPageFileName(),
      user_role: getUserRole(),
      timestamp: new Date().toISOString(),
      metadata: {
        referrer: document.referrer || '',
        viewport: window.innerWidth + 'x' + window.innerHeight
      }
    });
  }

  /**
   * trackContentView — 记录内容详情查看
   * @param {string} contentId - 内容ID，如 'OPP-0001'
   * @param {string} contentType - 类型：'opportunity' | 'demand'
   * @param {string} contentTitle - 内容标题
   */
  function trackContentView(contentId, contentType, contentTitle) {
    pushEvent({
      event_id: generateId(),
      event_type: 'content_view',
      page: contentType === 'opportunity' ? 'opp-detail' : 'req-detail',
      page_title: contentType === 'opportunity' ? '方案详情' : '需求详情',
      url: getCurrentPageFileName(),
      user_role: getUserRole(),
      timestamp: new Date().toISOString(),
      metadata: {
        content_id: contentId,
        content_type: contentType,
        content_title: contentTitle
      }
    });
  }

  /**
   * trackSearch — 记录搜索行为
   * @param {string} query - 搜索关键词
   * @param {number} resultCount - 搜索结果数
   * @param {string} page - 页面标识
   */
  function trackSearch(query, resultCount, page) {
    if (!query || !query.trim()) return;
    pushEvent({
      event_id: generateId(),
      event_type: 'search',
      page: page || 'unknown',
      page_title: '',
      url: getCurrentPageFileName(),
      user_role: getUserRole(),
      timestamp: new Date().toISOString(),
      metadata: {
        query: query.trim(),
        result_count: resultCount || 0
      }
    });
  }

  /**
   * trackClick — 记录点击行为
   * @param {string} elementLabel - 被点击元素描述
   * @param {string} page - 页面标识
   * @param {object} extra - 额外元数据
   */
  function trackClick(elementLabel, page, extra) {
    pushEvent({
      event_id: generateId(),
      event_type: 'click',
      page: page || 'unknown',
      page_title: '',
      url: getCurrentPageFileName(),
      user_role: getUserRole(),
      timestamp: new Date().toISOString(),
      metadata: Object.assign({ element: elementLabel }, extra || {})
    });
  }

  /**
   * trackEvent — 记录通用事件（AI功能等）
   * @param {string} eventName - 事件名称，如 'ai_chat', 'ai_recommend', 'ai_script', 'ai_analysis', 'ai_write', 'ai_search'
   * @param {object} extra - 额外元数据
   */
  function trackEvent(eventName, extra) {
    pushEvent({
      event_id: generateId(),
      event_type: eventName,
      page: extra && extra.page ? extra.page : 'unknown',
      page_title: '',
      url: getCurrentPageFileName(),
      user_role: getUserRole(),
      timestamp: new Date().toISOString(),
      metadata: extra || {}
    });
  }

  /**
   * startSession — 开始会话追踪，在页面关闭时自动上报
   */
  function startSession() {
    var sessionStart = SESSION_START;
    window.addEventListener('beforeunload', function() {
      var durationSec = Math.round((Date.now() - sessionStart) / 1000);
      // 只记录超过 3 秒的会话
      if (durationSec < 3) return;
      pushEvent({
        event_id: generateId(),
        event_type: 'session',
        page: 'session',
        page_title: '',
        url: getCurrentPageFileName(),
        user_role: getUserRole(),
        timestamp: new Date().toISOString(),
        metadata: {
          duration_sec: durationSec,
          page_count: Object.keys(lastPageView).length
        }
      });
    });
  }

  /* ========== 数据查询 API ========== */

  /**
   * queryEvents — 按条件查询事件
   */
  function queryEvents(filter) {
    var events = getEvents();
    var filterFn = filter || {};
    return events.filter(function(evt) {
      if (filterFn.event_type && evt.event_type !== filterFn.event_type) return false;
      if (filterFn.page && evt.page !== filterFn.page) return false;
      if (filterFn.user_role && evt.user_role !== filterFn.user_role) return false;
      if (filterFn.since) {
        var ts = new Date(evt.timestamp).getTime();
        if (ts < filterFn.since) return false;
      }
      if (filterFn.until) {
        var ts2 = new Date(evt.timestamp).getTime();
        if (ts2 > filterFn.until) return false;
      }
      return true;
    });
  }

  /**
   * getStats — 获取统计摘要
   */
  function getStats(days) {
    var daysAgo = days || 7;
    var since = Date.now() - daysAgo * 24 * 60 * 60 * 1000;
    var events = queryEvents({ since: since });

    var stats = {
      total_pv: 0,
      total_search: 0,
      total_click: 0,
      total_content_view: 0,
      total_session: 0,
      avg_session_sec: 0,
      page_breakdown: {},  // { pageKey: count }
      daily_breakdown: {},  // { 'YYYY-MM-DD': count }
      content_breakdown: {}, // { contentId: { title, count } }
      hourly_breakdown: {}  // { hour: count }
    };

    var sessionCount = 0, sessionTotal = 0;

    events.forEach(function(evt) {
      switch(evt.event_type) {
        case 'page_view': stats.total_pv++; break;
        case 'search': stats.total_search++; break;
        case 'click': stats.total_click++; break;
        case 'content_view': stats.total_content_view++; break;
        case 'session':
          stats.total_session++;
          if (evt.metadata && evt.metadata.duration_sec) {
            sessionCount++;
            sessionTotal += evt.metadata.duration_sec;
          }
          break;
      }

      // 页面分组
      if (evt.event_type === 'page_view' || evt.event_type === 'content_view') {
        var pg = evt.page || 'unknown';
        stats.page_breakdown[pg] = (stats.page_breakdown[pg] || 0) + 1;
      }

      // 日期分组
      var dateStr = evt.timestamp.slice(0, 10); // YYYY-MM-DD
      stats.daily_breakdown[dateStr] = (stats.daily_breakdown[dateStr] || 0) + 1;

      // 时段分组
      try {
        var hour = new Date(evt.timestamp).getHours();
        stats.hourly_breakdown[hour] = (stats.hourly_breakdown[hour] || 0) + 1;
      } catch(e) {}

      // 内容分组
      if (evt.event_type === 'content_view' && evt.metadata && evt.metadata.content_id) {
        var cid = evt.metadata.content_id;
        if (!stats.content_breakdown[cid]) {
          stats.content_breakdown[cid] = {
            title: evt.metadata.content_title || cid,
            type: evt.metadata.content_type || '',
            count: 0
          };
        }
        stats.content_breakdown[cid].count++;
      }
    });

    stats.avg_session_sec = sessionCount > 0 ? Math.round(sessionTotal / sessionCount) : 0;
    return stats;
  }

  /**
   * clearEvents — 清空所有埋点数据
   */
  function clearEvents() {
    localStorage.removeItem(STORAGE_KEY);
    lastPageView = {};
  }

  /**
   * exportCSV — 导出事件为 CSV 字符串
   */
  function exportCSV(filter) {
    var events = filter ? queryEvents(filter) : getEvents();
    if (events.length === 0) return '';
    var header = 'event_id,event_type,page,page_title,user_role,timestamp,metadata\n';
    var rows = events.map(function(evt) {
      return [
        evt.event_id,
        evt.event_type,
        evt.page,
        '"' + (evt.page_title || '').replace(/"/g,'""') + '"',
        evt.user_role,
        evt.timestamp,
        '"' + JSON.stringify(evt.metadata || {}).replace(/"/g,'""') + '"'
      ].join(',');
    });
    return header + rows.join('\n');
  }

  /**
   * getEventCount — 获取事件总数
   */
  function getEventCount() {
    return getEvents().length;
  }

  /* ========== 自动初始化 ========== */
  startSession();

  // 每天凌晨自动清理超过90天的数据
  (function scheduleCleanup() {
    var today = new Date().toDateString();
    var lastCleanup = localStorage.getItem('analytics_last_cleanup');
    if (lastCleanup !== today) {
      localStorage.setItem('analytics_last_cleanup', today);
      var cutoff = Date.now() - 90 * 24 * 60 * 60 * 1000;
      var events = getEvents().filter(function(evt) {
        return new Date(evt.timestamp).getTime() >= cutoff;
      });
      if (events.length < getEvents().length) {
        saveEvents(events);
      }
    }
  })();

  /* ========== 暴露全局 API ========== */
  window.DataTracker = {
    trackPageView: trackPageView,
    trackContentView: trackContentView,
    trackSearch: trackSearch,
    trackClick: trackClick,
    trackEvent: trackEvent,
    startSession: startSession,
    queryEvents: queryEvents,
    getStats: getStats,
    clearEvents: clearEvents,
    exportCSV: exportCSV,
    getEventCount: getEventCount
  };

  console.log('[DataTracker] 埋点系统已就绪 | 当前事件数: ' + getEventCount());
})();
