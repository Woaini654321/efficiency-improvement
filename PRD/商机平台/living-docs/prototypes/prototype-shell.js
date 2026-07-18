/**
 * Quectel 销售业务拓展咨询平台 · 原型导航外壳 v4 (prototype-shell.js)
 * 图标库：Tabler Icons webfont（线性 outline 风格）
 * v4: 评审后调整——"找方案"→"首页"聚合、需求互助定位、分享/关注功能开关、运营中心替发
 * 遵循 IIFE 隔离，禁止顶层 const/let，禁止 JSX
 */
(() => {
  var SHELL_CONFIG = {
    systemName: 'Quectel商机互助平台',
    systemSubtitle: '找方案 · 提需求 · 互助共赢',
    headerHeight: 56,
    /* ---- 用户端顶部菜单 ---- */
    topMenuItems: [
      { key: 'opp-hall', label: '首页', page: 'PAGE-PC-01_找方案.html' },
      { key: 'search-solution', label: '查方案', page: 'PAGE-PC-17_查方案.html' },
      { key: 'req-square', label: '提需求', page: 'PAGE-PC-04_提需求.html' },
      { key: 'discussion', label: '讨论区', page: 'PAGE-PC-18_讨论区.html' }
    ],
    topMenuIconMap: {
      'opp-hall': 'ti-home',
      'search-solution': 'ti-search',
      'req-square': 'ti-target',
      'discussion': 'ti-messages',
      'tools': 'ti-tool',
      'my-tasks': 'ti-checklist',
      'intel-center': 'ti-radar'
    },
    /* ---- 运营中心侧边菜单 ---- */
    adminSiderWidth: 220,
    adminCollapsedWidth: 64,
    adminMenuItems: [
      { key: 'dashboard', label: '运营看板', page: 'PAGE-PC-12_运营数据看板.html' },
      { key: 'content-audit', label: '内容审核', page: 'PAGE-PC-10_内容审核管理.html' },
      { key: 'deadline-monitor', label: '需求时效监控', page: 'PAGE-PC-13_SLA监控.html' },
      { key: 'meeting-task-mgmt', label: '会议任务管理', page: 'PAGE-PC-21_会议任务管理.html' },
      { key: 'batch-publish', label: '批量发布任务', page: 'PAGE-PC-23_批量发布任务.html' },
      { key: 'announcement', label: '公告发布', page: 'PAGE-PC-15_公告发布.html' },
      { key: 'category-mgmt', label: '数据字典', page: 'PAGE-PC-11_分类体系维护.html' },
      { key: 'audit-log', label: '操作日志', page: 'PAGE-PC-14_操作日志.html' }
    ],
    adminMenuIconMap: {
      'content-audit': 'ti-shield-check',
      'category-mgmt': 'ti-tags',
      'user-perm': 'ti-users',
      'dashboard': 'ti-chart-bar',
      'deadline-monitor': 'ti-clock',
      'meeting-task-mgmt': 'ti-clipboard-list',
      'batch-publish': 'ti-stack-2',
      'announcement': 'ti-speakerphone',
      'audit-log': 'ti-file-text'
    },
    /* ---- 所有页面key → 端口映射 ---- */
    pagePortMap: {
      'opp-hall': 'user',
      'search-solution': 'user',
      'req-square': 'user',
      'discussion': 'user',
      'tools': 'user',
      'intel-center': 'user',
      'notify-center': 'user',
      'notify-prefs': 'user',
      'profile': 'user',
      'my-tasks': 'user',
      'discussion-detail': 'user',
      'new-post': 'user',
      'venting': 'user',
      'intel-submit': 'user',
      'announcement-center': 'user',
      'content-audit': 'admin',
      'category-mgmt': 'admin',
      'user-perm': 'admin',
      'dashboard': 'admin',
      'deadline-monitor': 'admin',
      'meeting-task-mgmt': 'admin',
      'batch-publish': 'admin',
      'announcement': 'admin',
      'audit-log': 'admin',
    },
    currentUser: { name: 'San.Zhang 张三', role: '销售人员', avatar: null },

    /* ---- 全局搜索数据（vanilla JS，所有页面可用） ---- */
    searchData: {
      /* 36 条方案 */
      opps: (function() {
        var types = ['product_info', 'solution', 'success_case'];
        var titles = [
          'EC200A Cat.1模组低功耗方案', 'RG500Q 5G模组车载集成方案', 'BC260Y NB-IoT智慧表计案例',
          'AG525R 车载前装解决方案', 'EC800M Cat.1 DTU行业应用', 'SC66 智能模组Android方案',
          'RG200U Cat.1bis模组产品手册', 'FC41D Wi-Fi&BLE双模方案', 'EG912Y Cat.4模组海外部署案例',
          'BG95-M3 多模LPWA全球方案', 'LC29H GNSS双频定位方案', 'SG865W-WF 5G+WiFi6方案'
        ];
        var publishers = ['Tony.Zhang 张伟', 'Nina.Li 李娜', 'Kevin.Wang 王强', 'Yan.Chen 陈燕', 'Leo.Zhao 赵磊', 'Owen.Liu 刘洋'];
        var cats = [['Cat.1模组', 'IoT模组'], ['5G模组', '车载方案'], ['NB-IoT模组', '智慧表计'], ['车载方案'], ['Cat.1模组', 'IoT模组', '行业方案'], ['智能模组']];
        var typeLabels = { product_info: '产品信息', solution: '解决方案', success_case: '成功案例' };
        var arr = [];
        for (var i = 0; i < 36; i++) {
          var tp = types[i % 3];
          var days = Math.floor(Math.random() * 30);
          var d = new Date(); d.setDate(d.getDate() - days);
          arr.push({
            id: 'OPP-' + String(i + 1).padStart(4, '0'),
            title: titles[i % titles.length],
            summary: '本方案详细介绍了' + titles[i % titles.length] + '的技术特性、应用场景及客户收益。',
            type: tp,
            typeLabel: typeLabels[tp],
            categories: cats[i % cats.length],
            publisher: publishers[i % publishers.length],
            date: d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0'),
            source: 'opp'
          });
        }
        return arr;
      })(),
      /* 28 条需求 */
      reqs: (function() {
        var titles = [
          '急需5G模组在智慧城市场景下的技术方案', 'NB-IoT模组低功耗方案需求', '智慧城市5G网络覆盖技术选型',
          '车联网4G模组稳定性方案', '工业物联网WiFi模组需求', '5G SA/NSA双模模组选型对比',
          '智慧城市IoT平台对接方案咨询', '车载通信模组低功耗设计需求', '5G RedCap模组在工业场景的适配评估',
          '智慧园区WiFi6覆盖方案需求', 'Cat.1模组成本优化方案请求', 'GNSS高精度定位方案需求',
          '支付POS终端通信方案选型', '智能表计NB-IoT方案评估'
        ];
        var statuses = ['Pending', 'Collecting', 'Adopted'];
        var statusLabels = { Pending: '待收集', Collecting: '收集中', Adopted: '已采纳' };
        var publishers = ['Liu.Zhao 赵六', 'Qi.Qian 钱七', 'Ben.Sun 孙八', 'Joe.Zhou 周九', 'Simon.Wu 吴十'];
        var reqCats = [['IoT模组','5G模组'], ['NB-IoT模组'], ['5G模组','智慧表计'], ['车载方案'], ['IoT模组'], ['Cat.1模组'], ['行业方案'], ['天线产品']];
        var arr = [];
        for (var j = 0; j < 28; j++) {
          var st = statuses[j % 3];
          var days = Math.floor(Math.random() * 60);
          var d = new Date(); d.setDate(d.getDate() - days);
          arr.push({
            id: 'REQ-' + String(j + 1).padStart(3, '0'),
            title: titles[j % titles.length],
            summary: publishers[j % publishers.length] + '提交的需求：' + titles[j % titles.length] + '，期望在一周内得到方案反馈。',
            status: st,
            statusLabel: statusLabels[st],
            categories: reqCats[j % reqCats.length],
            type: 'requirement',
            publisher: publishers[j % publishers.length],
            date: d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0'),
            source: 'req'
          });
        }
        return arr;
      })(),
      /* 10 条讨论帖 */
      posts: (function() {
        var topics = ['商机讨论','经验分享','方案互助','行业动态','商机讨论','经验分享','方案互助','行业动态','商机讨论','吐槽'];
        var titles = [
          '5G专网在智慧工厂的落地机会探讨', '客户说价格太高，这个话术亲测有效',
          '求推荐车载前装过车规的5G模组', '2026年物联网模组市场趋势分析',
          '海外智慧城市项目的商机识别技巧', '如何高效准备客户技术交流presentation',
          'Cat.1模组在多省市智能电表项目的适配经验', '车载通信模组国产替代的最新进展',
          '大型政企客户的关系维护和二次开发策略', '物联网中台项目商机挖掘的几个信号'
        ];
        var authors = ['Tony.Zhang 张伟','Leo.Zhao 赵磊','Owen.Liu 刘洋','Nina.Li 李娜','Kevin.Wang 王强','Yan.Chen 陈燕','Liu.Zhao 赵六','Jing.Chen 陈静','Alice.Wang 王芳','Mark.Zhang 张强'];
        var tags = [['5G','智慧工厂'],['销售技巧','话术'],['车载','5G模组'],['行业趋势','5G'],['海外','智慧城市'],['技术交流','presentation'],['Cat.1','智能电表'],['车载','国产替代'],['政企客户','大客户'],['物联网','商机挖掘']];
        var arr = [];
        for (var k = 0; k < 10; k++) {
          var days2 = 30 - k * 3;
          var d2 = new Date(); d2.setDate(d2.getDate() - days2);
          arr.push({
            id: 'POST-' + String(k + 1).padStart(3, '0'),
            title: titles[k],
            content: '关于「' + titles[k] + '」的详细讨论内容...',
            topic: topics[k],
            author: authors[k],
            tags: tags[k],
            date: d2.getFullYear() + '-' + String(d2.getMonth()+1).padStart(2,'0') + '-' + String(d2.getDate()).padStart(2,'0'),
            replyCount: Math.floor(Math.random() * 30) + 3,
            viewCount: Math.floor(Math.random() * 500) + 100,
            source: 'post'
          });
        }
        return arr;
      })(),
    }
  };

  /* ========== 全局搜索 Modal（vanilla JS） ========== */
  var searchState = { keyword: '', showAdv: false, advType: '', advCategory: '' };

  function buildSearchOverlayHTML() {
    var h = '';
    h += '<div id="gs-overlay" style="display:none;position:fixed;inset:0;z-index:9999;background:rgba(0,0,0,0.45);align-items:flex-start;justify-content:center;padding-top:80px;" onclick="if(event.target===this)window.__closeGlobalSearch()">';
    h += '<div id="gs-panel" style="width:720px;max-height:80vh;background:#fff;border-radius:16px;box-shadow:0 12px 48px rgba(0,0,0,0.2);display:flex;flex-direction:column;overflow:hidden;animation:gsFadeIn 0.2s ease;" onclick="event.stopPropagation()">';
    /* Header: 搜索框 */
    h += '<div style="padding:20px 24px 0;">';
    h += '<div style="display:flex;align-items:center;gap:12px;">';
    h += '<div style="flex:1;position:relative;display:flex;align-items:center;">';
    h += '<i class="ti ti-search" style="position:absolute;left:14px;font-size:18px;color:#bfbfbf;pointer-events:none;z-index:1;"></i>';
    h += '<input id="gs-input" type="text" placeholder="搜索方案、需求、讨论..." style="width:100%;height:44px;padding:0 40px 0 42px;border:1px solid #e8e8e8;border-radius:10px;font-size:15px;outline:none;transition:border-color 0.2s;box-sizing:border-box;" onfocus="this.style.borderColor=\'#1890ff\'" onblur="this.style.borderColor=\'#e8e8e8\'" oninput="window.__handleSearchInput(this.value)" />';
    h += '<span id="gs-clear" style="display:none;position:absolute;right:10px;cursor:pointer;color:#bfbfbf;font-size:18px;line-height:1;" onclick="window.__clearSearch()">&times;</span>';
    h += '</div>';
    h += '<button style="width:36px;height:36px;border:none;background:transparent;cursor:pointer;border-radius:8px;display:flex;align-items:center;justify-content:center;color:#999;flex-shrink:0;transition:background 0.2s;margin-right:4px;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.__toggleAdvFilters()" title="高级筛选"><i class="ti ti-adjustments-horizontal" style="font-size:18px;"></i></button>';
    h += '<button style="width:36px;height:36px;border:none;background:transparent;cursor:pointer;border-radius:8px;display:flex;align-items:center;justify-content:center;color:#999;flex-shrink:0;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.__closeGlobalSearch()"><i class="ti ti-x" style="font-size:20px;"></i></button>';
    h += '</div>';
    h += '</div>';
    /* Advanced Filters */
    h += '<div id="gs-adv-panel" style="display:none;padding:12px 24px 0;border-bottom:1px solid #f0f0f0;">';
    h += '<div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center;">';
    h += '<select id="gs-adv-type" style="padding:6px 12px;border:1px solid #e8e8e8;border-radius:6px;font-size:13px;outline:none;background:#fff;" onchange="window.__handleAdvFilterChange()"><option value="">全部类型</option><option value="product_info">产品信息</option><option value="solution">解决方案</option><option value="success_case">成功案例</option><option value="requirement">方案需求</option><option value="discussion">讨论帖</option></select>';
    h += '<select id="gs-adv-cat" style="padding:6px 12px;border:1px solid #e8e8e8;border-radius:6px;font-size:13px;outline:none;background:#fff;" onchange="window.__handleAdvFilterChange()"><option value="">全部分类</option><option value="IoT模组">IoT模组</option><option value="Cat.1模组">Cat.1模组</option><option value="5G模组">5G模组</option><option value="车载方案">车载方案</option><option value="智慧表计">智慧表计</option><option value="天线产品">天线产品</option><option value="行业方案">行业方案</option></select>';
    h += '<button style="padding:6px 12px;border:1px solid #e8e8e8;background:#fff;border-radius:6px;font-size:12px;color:#8c8c8c;cursor:pointer;" onclick="window.__resetAdvFilters()">重置筛选</button>';
    h += '<span id="gs-filter-count" style="font-size:12px;color:#1677ff;margin-left:4px;"></span>';
    h += '</div>';
    h += '</div>';
    /* Search tab */
    h += '<div style="padding:8px 24px;border-bottom:1px solid #f0f0f0;font-size:14px;color:#595959;"><i class="ti ti-search" style="font-size:16px;margin-right:6px;color:#1890ff;"></i>站内搜索</div>';
    /* Results area */
    h += '<div id="gs-results" style="padding:0 24px 20px;min-height:200px;max-height:50vh;overflow-y:auto;">';
    h += '<div id="gs-empty-state" style="text-align:center;padding:60px 0;color:#bfbfbf;">';
    h += '<i class="ti ti-search" style="font-size:40px;display:block;margin-bottom:12px;"></i>';
    h += '<div style="font-size:14px;">输入关键词搜索方案、需求、讨论</div>';
    h += '</div>';
    h += '<div id="gs-loading" style="display:none;text-align:center;padding:60px 0;"></div>';
    h += '<div id="gs-result-list" style="display:none;"></div>';
    h += '<div id="gs-saved-searches" style="border-top:1px solid #f0f0f0;padding:12px 0;display:none;"></div>';
    h += '</div>';
    h += '</div></div>';
    return h;
  }

  function highlightKeyword(text, keyword) {
    if (!keyword || keyword.length < 1) return text;
    var escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return text.replace(new RegExp('(' + escaped + ')', 'gi'), '<mark style="background:#fff3b0;color:#1a1a2e;padding:0 2px;border-radius:2px;">$1</mark>');
  }

  function buildResultCard(item) {
    var isOpp = item.source === 'opp';
    var isReq = item.source === 'req';
    var isPost = item.source === 'post';
    if (isPost) {
      var tagColors = { '商机讨论': '#1677ff', '方案互助': '#fa8c16', '经验分享': '#52c41a', '行业动态': '#722ed1', '吐槽': '#eb2f96' };
      var tagColor = tagColors[item.topic] || '#1677ff';
      var h = '';
      h += '<div class="gs-result-item" style="display:flex;align-items:flex-start;gap:14px;padding:14px 16px;border-radius:10px;cursor:pointer;transition:all 0.2s;border-left:3px solid transparent;" onmouseover="this.style.background=\'#f0f5ff\';this.style.borderLeftColor=\'#1890ff\';" onmouseout="this.style.background=\'transparent\';this.style.borderLeftColor=\'transparent\';" onclick="window.open(\'PAGE-PC-18_讨论区.html\',\'_blank\')">';
      h += '<div style="width:40px;height:40px;border-radius:10px;background:linear-gradient(135deg, #f0f5ff, #d6e4ff);display:flex;align-items:center;justify-content:center;flex-shrink:0;">';
      h += '<i class="ti ti-messages" style="font-size:18px;color:#1677ff;"></i>';
      h += '</div>';
      h += '<div style="flex:1;min-width:0;">';
      h += '<div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">';
      h += '<span style="display:inline-flex;align-items:center;gap:4px;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:500;color:' + tagColor + ';background:' + tagColor + '15;border:1px solid ' + tagColor + '40;">' + item.topic + '</span>';
      h += '<span style="font-size:14px;font-weight:600;color:#1a1a2e;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + highlightKeyword(item.title, searchState.keyword) + '</span>';
      h += '</div>';
      h += '<div style="font-size:12px;color:#8c8c8c;margin-bottom:4px;">' + item.author + ' · ' + item.replyCount + ' 回复 · ' + item.viewCount + ' 浏览 · ' + item.date + '</div>';
      h += '</div>';
      h += '</div>';
      return h;
    }

    // opp/req (existing)
    var badgeStyle = isOpp
      ? 'color:#1890ff;background:#e6f7ff;border:1px solid #91d5ff;'
      : 'color:#722ed1;background:#f9f0ff;border:1px solid #d3adf7;';
    var gradBg = isOpp
      ? 'linear-gradient(135deg, #e6f7ff, #bae7ff)'
      : 'linear-gradient(135deg, #f9f0ff, #d3adf7)';
    var iconClass = isOpp ? 'ti-file-text' : 'ti-help';
    var iconColor = isOpp ? '#1890ff' : '#722ed1';
    var href = isOpp
      ? 'PAGE-PC-02_商机详情页.html?id=' + item.id
      : 'PAGE-PC-05_需求详情页.html?id=' + item.id;
    var typeLabel = isOpp ? (item.typeLabel || '方案') : (item.statusLabel || '需求');

    var h = '';
    h += '<div class="gs-result-item" style="display:flex;align-items:flex-start;gap:14px;padding:14px 16px;border-radius:10px;cursor:pointer;transition:all 0.2s;border-left:3px solid transparent;" onmouseover="this.style.background=\'#f0f5ff\';this.style.borderLeftColor=\'#1890ff\';" onmouseout="this.style.background=\'transparent\';this.style.borderLeftColor=\'transparent\';" onclick="window.open(\'' + href + '\',\'_blank\')">';
    h += '<div style="width:40px;height:40px;border-radius:10px;background:' + gradBg + ';display:flex;align-items:center;justify-content:center;flex-shrink:0;">';
    h += '<i class="ti ' + iconClass + '" style="font-size:18px;color:' + iconColor + ';"></i>';
    h += '</div>';
    h += '<div style="flex:1;min-width:0;">';
    h += '<div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">';
    h += '<span style="display:inline-flex;align-items:center;gap:4px;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:500;' + badgeStyle + '">' + typeLabel + '</span>';
    h += '<span style="font-size:14px;font-weight:600;color:#1a1a2e;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + highlightKeyword(item.title, searchState.keyword) + '</span>';
    h += '</div>';
    h += '<div style="font-size:12px;color:#8c8c8c;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:4px;">' + highlightKeyword(item.summary, searchState.keyword) + '</div>';
    h += '<div style="font-size:11px;color:#bfbfbf;">' + item.publisher + ' · ' + item.date + '</div>';
    h += '</div>';
    h += '</div>';
    return h;
  }

  function renderSearchResults() {
    var resultsEl = document.getElementById('gs-result-list');
    var emptyEl = document.getElementById('gs-empty-state');
    var loadingEl = document.getElementById('gs-loading');
    var keyword = searchState.keyword;

    if (!resultsEl || !emptyEl || !loadingEl) return;

    // Reset
    resultsEl.style.display = 'none';
    resultsEl.innerHTML = '';
    emptyEl.style.display = 'none';
    loadingEl.style.display = 'none';

    if (!keyword) {
      emptyEl.innerHTML = '<i class="ti ti-search" style="font-size:40px;display:block;margin-bottom:12px;color:#bfbfbf;"></i><div style="font-size:14px;color:#bfbfbf;">输入关键词搜索方案、需求、讨论</div>';
      emptyEl.style.display = '';
      return;
    }
      var kw = keyword.toLowerCase();
      var oppResults = SHELL_CONFIG.searchData.opps.filter(function(d) {
        if (kw && d.title.toLowerCase().indexOf(kw)===-1 && d.summary.toLowerCase().indexOf(kw)===-1) return false;
        if (searchState.advType && searchState.advType!=='requirement' && searchState.advType!=='discussion' && d.type!==searchState.advType) return false;
        if (searchState.advCategory && (d.categories||[]).indexOf(searchState.advCategory)===-1) return false;
        return true;
      }).slice(0, 6);
      var reqResults = SHELL_CONFIG.searchData.reqs.filter(function(d) {
        if (kw && d.title.toLowerCase().indexOf(kw)===-1 && d.summary.toLowerCase().indexOf(kw)===-1) return false;
        if (searchState.advType==='product_info'||searchState.advType==='solution'||searchState.advType==='success_case') return false;
        if (searchState.advCategory && (d.categories||[]).indexOf(searchState.advCategory)===-1) return false;
        return true;
      }).slice(0, 3);
      var postResults = SHELL_CONFIG.searchData.posts.filter(function(d) {
        if (kw && d.title.toLowerCase().indexOf(kw)===-1) return false;
        if (searchState.advType==='product_info'||searchState.advType==='solution'||searchState.advType==='success_case'||searchState.advType==='requirement') return false;
        return true;
      }).slice(0, 3);
      var all = oppResults.concat(reqResults, postResults);

      if (all.length === 0) {
        emptyEl.innerHTML = '<div style="padding:40px 0;text-align:center;"><i class="ti ti-file-off" style="font-size:40px;display:block;margin-bottom:12px;color:#d9d9d9;"></i><div style="font-size:14px;color:#bfbfbf;">未找到相关内容，换个关键词试试</div></div>';
        emptyEl.style.display = '';
        return;
      }
      resultsEl.innerHTML = all.map(function(item) { return buildResultCard(item); }).join('');
      resultsEl.style.display = '';
      // 自动保存搜索条件
      window.__saveSearch();
  }

  /* ---- 全局搜索公开方法 ---- */
  window.__openGlobalSearch = function() {
    var overlay = document.getElementById('gs-overlay');
    if (!overlay) return;
    overlay.style.display = 'flex';
    setTimeout(function() {
      var input = document.getElementById('gs-input');
      if (input) { input.focus(); input.value = ''; }
      searchState.keyword = '';
      searchState.showAdv = false;
      searchState.advType = '';
      searchState.advCategory = '';
      // Reset adv panel
      var advP = document.getElementById('gs-adv-panel'); if (advP) advP.style.display = 'none';
      var advT = document.getElementById('gs-adv-type'); if (advT) advT.value = '';
      var advC = document.getElementById('gs-adv-cat'); if (advC) advC.value = '';
      var fcnt = document.getElementById('gs-filter-count'); if (fcnt) fcnt.textContent = '';
      // Reset clear button
      var clr = document.getElementById('gs-clear');
      if (clr) clr.style.display = 'none';
      renderSearchResults();
      renderSavedSearches();
    }, 100);
  };

  window.__closeGlobalSearch = function() {
    var overlay = document.getElementById('gs-overlay');
    if (overlay) overlay.style.display = 'none';
  };

  window.__handleSearchInput = function(val) {
    searchState.keyword = val;
    var clr = document.getElementById('gs-clear');
    if (clr) clr.style.display = val ? '' : 'none';

    renderSearchResults();
  };

  window.__clearSearch = function() {
    var input = document.getElementById('gs-input');
    if (input) { input.value = ''; input.focus(); }
    var clr = document.getElementById('gs-clear');
    if (clr) clr.style.display = 'none';
    searchState.keyword = '';
    renderSearchResults();
  };

  window.__toggleAdvFilters = function() {
    searchState.showAdv = !searchState.showAdv;
    var panel = document.getElementById('gs-adv-panel');
    if (panel) panel.style.display = searchState.showAdv ? '' : 'none';
  };

  window.__handleAdvFilterChange = function() {
    var t = document.getElementById('gs-adv-type');
    var c = document.getElementById('gs-adv-cat');
    searchState.advType = t ? t.value : '';
    searchState.advCategory = c ? c.value : '';
    var fcnt = document.getElementById('gs-filter-count');
    var count = (searchState.advType ? 1 : 0) + (searchState.advCategory ? 1 : 0);
    if (fcnt) fcnt.textContent = count > 0 ? '(' + count + ' 项筛选生效)' : '';
    renderSearchResults();
  };

  window.__resetAdvFilters = function() {
    var t = document.getElementById('gs-adv-type');
    var c = document.getElementById('gs-adv-cat');
    if (t) t.value = '';
    if (c) c.value = '';
    searchState.advType = '';
    searchState.advCategory = '';
    var fcnt = document.getElementById('gs-filter-count');
    if (fcnt) fcnt.textContent = '';
    renderSearchResults();
  };

  window.__saveSearch = function() {
    var keyword = (searchState.keyword || '').trim();
    if (!keyword && !searchState.advType && !searchState.advCategory) return;
    // 去重：相同条件不重复保存
    var raw = localStorage.getItem('saved_searches');
    var list = raw ? JSON.parse(raw) : [];
    var dup = list.some(function(s) {
      return s.keyword === searchState.keyword && s.advType === searchState.advType && s.advCategory === searchState.advCategory;
    });
    if (dup) { renderSavedSearches(); return; }
    var label = keyword || '(仅筛选条件)';
    if (searchState.advType) label += ' 类型:' + searchState.advType;
    if (searchState.advCategory) label += ' 分类:' + searchState.advCategory;
    if (list.length >= 5) { list.shift(); }
    list.push({ label: label, keyword: searchState.keyword, advType: searchState.advType, advCategory: searchState.advCategory });
    localStorage.setItem('saved_searches', JSON.stringify(list));
    renderSavedSearches();
  };

  window.__deleteSavedSearch = function(idx) {
    var raw = localStorage.getItem('saved_searches');
    var list = raw ? JSON.parse(raw) : [];
    list.splice(idx, 1);
    localStorage.setItem('saved_searches', JSON.stringify(list));
    renderSavedSearches();
  };

  window.__applySavedSearch = function(idx) {
    var raw = localStorage.getItem('saved_searches');
    var list = raw ? JSON.parse(raw) : [];
    var s = list[idx];
    if (!s) return;
    var input = document.getElementById('gs-input');
    if (input) { input.value = s.keyword || ''; }
    searchState.keyword = s.keyword || '';
    searchState.advType = s.advType || '';
    searchState.advCategory = s.advCategory || '';
    var clr = document.getElementById('gs-clear');
    if (clr) clr.style.display = searchState.keyword ? '' : 'none';
    var t = document.getElementById('gs-adv-type');
    var c = document.getElementById('gs-adv-cat');
    if (t) t.value = searchState.advType;
    if (c) c.value = searchState.advCategory;
    var fcnt = document.getElementById('gs-filter-count');
    var count = (searchState.advType ? 1 : 0) + (searchState.advCategory ? 1 : 0);
    if (fcnt) fcnt.textContent = count > 0 ? '(' + count + ' 项筛选生效)' : '';
    if (searchState.advType || searchState.advCategory) {
      searchState.showAdv = true;
      var panel = document.getElementById('gs-adv-panel');
      if (panel) panel.style.display = '';
    }
    if (false) {
      window.__switchSearchTab(s.tab);
    } else {
      renderSearchResults();
    }
  };

  function renderSavedSearches() {
    var container = document.getElementById('gs-saved-searches');
    if (!container) return;
    var raw = localStorage.getItem('saved_searches');
    var list = raw ? JSON.parse(raw) : [];
    if (list.length === 0) { container.style.display = 'none'; return; }
    container.style.display = '';
    var h = '<div style="font-size:12px;font-weight:600;color:#8c8c8c;margin-bottom:6px;"><i class="ti ti-bookmark" style="font-size:14px;margin-right:4px;vertical-align:-2px;"></i>已保存的搜索 (' + list.length + ')</div>';
    list.forEach(function(s, i) {
      h += '<div style="display:flex;align-items:center;justify-content:space-between;padding:6px 8px;border-radius:6px;cursor:pointer;transition:background 0.2s;font-size:13px;color:#595959;" onmouseover="this.style.background=\'#f0f5ff\'" onmouseout="this.style.background=\'transparent\'">';
      h += '<span onclick="window.__applySavedSearch(' + i + ')" style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + s.label + '</span>';
      h += '<span onclick="event.stopPropagation();window.__deleteSavedSearch(' + i + ')" style="color:#bfbfbf;cursor:pointer;font-size:14px;margin-left:8px;" title="删除">&times;</span>';
      h += '</div>';
    });
    container.innerHTML = h;
  }

  /* ========== 用户端 Shell（顶部导航） ========== */
  function renderUserShell(currentPageKey) {
    var headerHeight = SHELL_CONFIG.headerHeight;
    var originalContent = document.body.innerHTML;

    var html = '';
    html += '<style>';
    html += '@keyframes gsFadeIn{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:translateY(0);}}';
    html += '@keyframes gsSpin{from{transform:rotate(0deg);}to{transform:rotate(360deg);}}';
    html += '@keyframes mobileNavSlideIn{from{opacity:0;transform:translateY(-12px);}to{opacity:1;transform:translateY(0);}}';
    /* 响应式 */
    html += '@media(max-width:768px){';
    html += '#proto-header{padding:0 16px!important;height:52px!important;}';
    html += '#proto-header .shell-logo{margin-right:auto!important;}';
    html += '#proto-header .shell-logo span{font-size:14px!important;display:inline!important;}';
    html += '#proto-header .shell-top-menu{display:none!important;}';
    html += '#proto-header .shell-hamburger{display:flex!important;margin-right:10px!important;}';
    html += '#proto-header .shell-lang-switch{display:none!important;}';
    html += '#proto-header .shell-user-name{display:none!important;}';
    html += '#proto-header .shell-logo>div{width:28px!important;height:28px!important;font-size:14px!important;border-radius:6px!important;}';
    html += '#proto-content{padding:16px!important;max-width:100%!important;}';
    html += '#shell-mobile-nav{display:none;}';
    html += '#shell-mobile-nav.open{display:block!important;animation:mobileNavSlideIn 0.25s ease;}';
    html += '#gs-panel{width:100vw!important;max-height:100vh!important;border-radius:0!important;}';
    html += '#gs-overlay{padding-top:0!important;align-items:stretch!important;}';
    html += '#gs-results{max-height:55vh!important;padding:0 16px 16px!important;}';
    html += '#gs-panel>div:first-child{padding:16px 16px 0!important;}';
    html += '#gs-panel>div:nth-child(3){padding:8px 16px 0!important;}';
    html += '.gs-result-item{padding:12px 10px!important;}';
    html += '#gs-input{height:40px!important;font-size:14px!important;}';
    html += '}';
    html += '@media(min-width:769px) and (max-width:1024px){';
    html += '#proto-header{padding:0 24px!important;}';
    html += '#proto-header .top-menu-item{padding:0 14px!important;font-size:13px!important;}';
    html += '#proto-header .shell-logo{margin-right:24px!important;}';
    html += '#proto-content{padding:20px!important;max-width:100%!important;}';
    html += '#gs-panel{width:88vw!important;}';
    html += '}';
    html += '@media(min-width:769px){';
    html += '#shell-mobile-nav{display:none!important;}';
    html += '#proto-header .shell-hamburger{display:none!important;}';
    html += '}';
    html += '</style>';
    html += '<div id="proto-shell" style="min-height:100vh;background:#f5f5f5;">';

    /* --- Header --- */
    html += '<div id="proto-header" style="height:' + headerHeight + 'px;background:#fff;display:flex;align-items:center;padding:0 32px;box-shadow:0 1px 4px rgba(0,21,41,0.08);position:sticky;top:0;z-index:100;">';

    /* 汉堡菜单按钮（仅移动端显示） */
    html += '<div class="shell-hamburger" style="display:none;width:36px;height:36px;align-items:center;justify-content:center;cursor:pointer;border-radius:6px;margin-right:8px;flex-shrink:0;transition:background 0.2s;" onclick="window.__toggleMobileNav()" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'">';
    html += '<i class="ti ti-menu-2" style="font-size:22px;color:#595959;"></i>';
    html += '</div>';

    /* Logo */
    html += '<div class="shell-logo" style="display:flex;align-items:center;gap:10px;cursor:pointer;margin-right:40px;" onclick="window.location.href=\'PAGE-PC-01_找方案.html\'">';
    html += '<div style="width:32px;height:32px;background:linear-gradient(135deg,#1890ff,#36cfc9);border-radius:8px;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700;font-size:16px;flex-shrink:0;">Q</div>';
    html += '<span style="font-size:15px;font-weight:600;color:#1a1a2e;white-space:nowrap;">Quectel商机互助</span>';
    html += '<span style="font-size:11px;color:#8c8c8c;white-space:nowrap;margin-left:2px;padding-top:2px;">找方案 · 提需求 · 互助共赢</span>';
    html += '</div>';

    /* 顶部菜单项 */
    html += '<div class="shell-top-menu" style="display:flex;align-items:center;">';
    SHELL_CONFIG.topMenuItems.forEach(function(item) {
      var isActive = item.key === currentPageKey;
      var color = isActive ? '#1890ff' : '#595959';
      var fontWeight = isActive ? '600' : '400';
      var borderBottom = isActive ? '2px solid #1890ff' : '2px solid transparent';
      var iconClass = SHELL_CONFIG.topMenuIconMap[item.key] || '';
      html += '<div class="top-menu-item" data-key="' + item.key + '" style="height:' + headerHeight + 'px;display:flex;align-items:center;padding:0 20px;cursor:pointer;color:' + color + ';font-weight:' + fontWeight + ';font-size:14px;border-bottom:' + borderBottom + ';transition:all 0.2s;position:relative;" ';
      html += 'onmouseover="if(this.dataset.key!==\'' + currentPageKey + '\'){this.style.color=\'#1890ff\';this.style.borderBottomColor=\'#1890ff\';}" ';
      html += 'onmouseout="if(this.dataset.key!==\'' + currentPageKey + '\'){this.style.color=\'#595959\';this.style.borderBottomColor=\'transparent\';}" ';
      html += 'onclick="window.location.href=\'' + item.page + '\'">';
      html += '<i class="ti ' + iconClass + '" style="font-size:18px;margin-right:6px;"></i>';
      html += item.label;
      html += '</div>';
    });
    html += '</div>'; // end shell-top-menu

    /* 弹簧占位 */
    html += '<div style="flex:1;"></div>';
    html += '<div style="flex:1;"></div>';

    /* 右侧工具区 */
    html += '<div style="display:flex;align-items:center;gap:8px;">';

    /* 搜索 */
    html += '<div style="cursor:pointer;padding:8px;border-radius:6px;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" title="搜索" onclick="window.__openGlobalSearch()"><i class="ti ti-search" style="font-size:20px;color:#666;"></i></div>';

    /* 通知铃铛 */
    html += '<div style="position:relative;cursor:pointer;padding:8px;border-radius:6px;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-07_通知中心.html\'" title="消息中心">';
    html += '<i class="ti ti-bell" style="font-size:20px;color:#666;"></i>';
    html += '<span id="shell-notify-badge" style="position:absolute;top:4px;right:4px;background:#ff4d4f;color:#fff;font-size:10px;min-width:16px;height:16px;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0 4px;">5</span>';

    // 动态通知徽章更新函数（PC-07调用，同时更新用户端和运营中心）
    window.__updateNotifyBadge = function(count) {
      var badges = [document.getElementById('shell-notify-badge'), document.getElementById('shell-notify-badge-admin'), document.getElementById('shell-mobile-notify-badge')];
      badges.forEach(function(badge) {
        if (!badge) return;
        if (count <= 0) {
          badge.style.display = 'none';
        } else {
          badge.style.display = 'flex';
          badge.textContent = count > 99 ? '99+' : String(count);
        }
      });
    };
    html += '</div>';

    /* 语言 */
    html += '<div class="shell-lang-switch" style="cursor:pointer;padding:8px;border-radius:6px;font-size:13px;color:#666;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'">中/EN</div>';

    /* 用户头像（下拉菜单） */
    html += '<div id="avatar-wrap" style="position:relative;">';
    html += '<div id="avatar-trigger" style="display:flex;align-items:center;gap:8px;cursor:pointer;padding:6px 12px;border-radius:8px;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.__toggleAvatarMenu()">';
    html += '<div style="width:30px;height:30px;border-radius:50%;background:linear-gradient(135deg,#1890ff,#36cfc9);display:flex;align-items:center;justify-content:center;color:#fff;font-size:13px;font-weight:600;">' + SHELL_CONFIG.currentUser.name.charAt(0) + '</div>';
    html += '<span class="shell-user-name" style="font-size:13px;color:#333;max-width:80px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + SHELL_CONFIG.currentUser.name + '</span>';
    html += '<span style="font-size:10px;color:#999;">▾</span>';
    html += '</div>';

    /* 下拉菜单 */
    html += '<div id="avatar-dropdown" style="display:none;position:absolute;top:calc(100% + 4px);right:0;min-width:180px;background:#fff;border-radius:10px;box-shadow:0 6px 24px rgba(0,0,0,0.12);padding:6px 0;z-index:200;">';
    /* 用户信息 */
    html += '<div style="padding:12px 16px;border-bottom:1px solid #f0f0f0;">';
    html += '<div style="font-size:14px;font-weight:600;color:#1a1a2e;">' + SHELL_CONFIG.currentUser.name + '</div>';
    html += '<div style="font-size:12px;color:#8c8c8c;margin-top:2px;">' + SHELL_CONFIG.currentUser.role + '</div>';
    html += '</div>';
    /* 个人中心 */
    html += '<div class="dropdown-item" style="padding:10px 16px;cursor:pointer;font-size:13px;color:#333;display:flex;align-items:center;gap:8px;transition:background 0.15s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-08_个人中心.html\'">';
    html += '<i class="ti ti-user" style="font-size:16px;color:#8c8c8c;"></i>个人中心</div>';
    /* 切换到运营中心 */
    html += '<div style="height:1px;background:#f0f0f0;margin:4px 0;"></div>';
    html += '<div class="dropdown-item" style="padding:10px 16px;cursor:pointer;font-size:13px;color:#333;display:flex;align-items:center;gap:8px;transition:background 0.15s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-10_内容审核管理.html\'">';
    html += '<i class="ti ti-settings" style="font-size:16px;color:#8c8c8c;"></i>切换到运营中心</div>';
    /* 退出登录 */
    html += '<div style="height:1px;background:#f0f0f0;margin:4px 0;"></div>';
    html += '<div class="dropdown-item" style="padding:10px 16px;cursor:pointer;font-size:13px;color:#ff4d4f;display:flex;align-items:center;gap:8px;transition:background 0.15s;" onmouseover="this.style.background=\'#fff2f0\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-00_登录页.html\'">';
    html += '<i class="ti ti-logout" style="font-size:16px;color:#ff4d4f;"></i>退出登录</div>';
    html += '</div>'; // end dropdown

    html += '</div>'; // end avatar-wrap
    html += '</div>'; // end right tools
    html += '</div>'; // end header

    /* --- 移动端导航面板 --- */
    html += '<div id="shell-mobile-nav" style="display:none;position:fixed;top:' + headerHeight + 'px;left:0;right:0;background:#fff;z-index:99;box-shadow:0 4px 12px rgba(0,0,0,0.1);padding:8px 0;overflow-y:auto;max-height:calc(100vh - ' + headerHeight + 'px);">';
    SHELL_CONFIG.topMenuItems.forEach(function(item) {
      var isActive = item.key === currentPageKey;
      var bgColor = isActive ? '#e6f7ff' : 'transparent';
      var textColor = isActive ? '#1890ff' : '#333';
      var fontWeight = isActive ? '600' : '400';
      var iconClass = SHELL_CONFIG.topMenuIconMap[item.key] || '';
      html += '<div style="display:flex;align-items:center;gap:12px;padding:14px 20px;cursor:pointer;background:' + bgColor + ';color:' + textColor + ';font-weight:' + fontWeight + ';font-size:15px;transition:background 0.2s;" onmouseover="if(this.style.background===\'transparent\'||this.style.background===\'\')this.style.background=\'#f5f5f5\'" onmouseout="if(this.style.background===\'rgb(245,245,245)\')this.style.background=\'transparent\'" onclick="window.__closeMobileNav();window.location.href=\'' + item.page + '\'">';
      html += '<i class="ti ' + iconClass + '" style="font-size:20px;width:24px;text-align:center;color:' + (isActive ? '#1890ff' : '#8c8c8c') + ';"></i>';
      html += item.label;
      html += '</div>';
    });
    // 分隔线 + 辅助链接
    html += '<div style="height:1px;background:#f0f0f0;margin:8px 0;"></div>';
    html += '<div style="display:flex;align-items:center;gap:12px;padding:14px 20px;cursor:pointer;font-size:15px;color:#333;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.__closeMobileNav();window.location.href=\'PAGE-PC-07_通知中心.html\'">';
    html += '<i class="ti ti-bell" style="font-size:20px;width:24px;text-align:center;color:#8c8c8c;"></i>通知中心';
    html += '<span id="shell-mobile-notify-badge" style="background:#ff4d4f;color:#fff;font-size:10px;min-width:18px;height:18px;border-radius:9px;display:flex;align-items:center;justify-content:center;padding:0 5px;margin-left:4px;">5</span>';
    html += '</div>';
    html += '<div style="display:flex;align-items:center;gap:12px;padding:14px 20px;cursor:pointer;font-size:15px;color:#333;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.__closeMobileNav();window.location.href=\'PAGE-PC-08_个人中心.html\'">';
    html += '<i class="ti ti-user" style="font-size:20px;width:24px;text-align:center;color:#8c8c8c;"></i>个人中心';
    html += '</div>';
    html += '</div>'; // end mobile nav

    /* --- Content --- */
    html += '<div id="proto-content" style="padding:20px 32px;min-height:calc(100vh - ' + headerHeight + 'px);max-width:1400px;margin:0 auto;">';
    html += '</div>';

    html += '</div>'; // end shell

    /* 全局搜索浮层 */
    html += buildSearchOverlayHTML();


    document.body.innerHTML = html;

    // Move original content
    var contentArea = document.getElementById('proto-content');
    var tempDiv = document.createElement('div');
    tempDiv.innerHTML = originalContent;
    while (tempDiv.firstChild) {
      contentArea.appendChild(tempDiv.firstChild);
    }

    // Avatar dropdown toggle
    window.__toggleAvatarMenu = function() {
      var dd = document.getElementById('avatar-dropdown');
      dd.style.display = dd.style.display === 'none' ? 'block' : 'none';
    };
    // Click outside to close
    document.addEventListener('click', function(e) {
      var wrap = document.getElementById('avatar-wrap');
      var dd = document.getElementById('avatar-dropdown');
      if (wrap && dd && !wrap.contains(e.target)) {
        dd.style.display = 'none';
      }
    });

    // 移动端导航
    window.__toggleMobileNav = function() {
      var nav = document.getElementById('shell-mobile-nav');
      if (!nav) return;
      if (nav.classList.contains('open')) {
        nav.classList.remove('open');
        nav.style.display = 'none';
      } else {
        nav.classList.add('open');
        nav.style.display = 'block';
      }
    };
    window.__closeMobileNav = function() {
      var nav = document.getElementById('shell-mobile-nav');
      if (nav) { nav.classList.remove('open'); nav.style.display = 'none'; }
    };

  }

  /* ========== 运营中心 Shell（左侧导航） ========== */
  function renderAdminShell(currentPageKey) {
    var siderWidth = SHELL_CONFIG.adminSiderWidth;
    var collapsedWidth = SHELL_CONFIG.adminCollapsedWidth;
    var headerHeight = SHELL_CONFIG.headerHeight;
    var collapsed = false;
    var originalContent = document.body.innerHTML;

    var html = '';
    html += '<style>@keyframes gsFadeIn{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:translateY(0);}}@keyframes gsSpin{from{transform:rotate(0deg);}to{transform:rotate(360deg);}}';
    html += '</style>';
    html += '<div id="proto-shell" style="display:flex;min-height:100vh;background:#f5f5f5;">';

    /* --- Sider --- */
    html += '<div id="proto-sider" style="width:' + siderWidth + 'px;min-height:100vh;background:linear-gradient(180deg,#001529 0%,#002140 100%);transition:width 0.2s;position:fixed;left:0;top:0;bottom:0;z-index:100;overflow-y:auto;overflow-x:hidden;">';

    /* Logo */
    html += '<div class="sidebar-logo" style="height:' + headerHeight + 'px;display:flex;align-items:center;padding:0 16px;border-bottom:1px solid rgba(255,255,255,0.1);cursor:pointer;" onclick="window.location.href=\'PAGE-PC-10_内容审核管理.html\'">';
    html += '<div style="width:32px;height:32px;background:linear-gradient(135deg,#1890ff,#36cfc9);border-radius:8px;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700;font-size:16px;flex-shrink:0;">Q</div>';
    html += '<span id="logo-text" style="margin-left:12px;color:#fff;font-size:14px;font-weight:600;white-space:nowrap;overflow:hidden;">运营中心</span>';
    html += '</div>';

    /* Menu items */
    html += '<div style="padding:12px 0;">';
    SHELL_CONFIG.adminMenuItems.forEach(function(item) {
      var isActive = item.key === currentPageKey;
      var bgColor = isActive ? 'rgba(24,144,255,0.15)' : 'transparent';
      var textColor = isActive ? '#1890ff' : 'rgba(255,255,255,0.75)';
      var borderLeft = isActive ? '3px solid #1890ff' : '3px solid transparent';
      var fontWeight = isActive ? '600' : '400';
      var iconClass = SHELL_CONFIG.adminMenuIconMap[item.key] || '';

      html += '<div class="menu-item" data-key="' + item.key + '" style="padding:10px 16px 10px 21px;cursor:pointer;display:flex;align-items:center;gap:10px;border-left:' + borderLeft + ';background:' + bgColor + ';color:' + textColor + ';font-weight:' + fontWeight + ';transition:all 0.2s;font-size:14px;white-space:nowrap;overflow:hidden;" ';
      html += 'onmouseover="if(this.dataset.key!==\'' + currentPageKey + '\'){this.style.background=\'rgba(255,255,255,0.06)\';this.style.color=\'#fff\';}" ';
      html += 'onmouseout="if(this.dataset.key!==\'' + currentPageKey + '\'){this.style.background=\'transparent\';this.style.color=\'rgba(255,255,255,0.75)\';}" ';
      html += 'onclick="window.location.href=\'' + item.page + '\'">';
      html += '<i class="ti ' + iconClass + '" style="font-size:18px;width:18px;text-align:center;flex-shrink:0;"></i>';
      html += '<span class="menu-label">' + item.label + '</span>';
      html += '</div>';
    });
    html += '</div>';

    /* Collapse toggle */
    html += '<div id="collapse-trigger" style="position:absolute;bottom:0;left:0;right:0;height:48px;display:flex;align-items:center;justify-content:center;cursor:pointer;color:rgba(255,255,255,0.5);border-top:1px solid rgba(255,255,255,0.1);transition:color 0.2s;" onmouseover="this.style.color=\'#fff\'" onmouseout="this.style.color=\'rgba(255,255,255,0.5)\'" onclick="window.__toggleSider && window.__toggleSider()">';
    html += '<i class="ti ti-chevron-left" id="collapse-icon" style="font-size:16px;"></i>';
    html += '</div>';

    html += '</div>'; // end sider

    /* --- Main area --- */
    html += '<div id="proto-main" style="margin-left:' + siderWidth + 'px;flex:1;min-height:100vh;transition:margin-left 0.2s;">';

    /* Header */
    html += '<div id="proto-header" style="height:' + headerHeight + 'px;background:#fff;display:flex;align-items:center;justify-content:space-between;padding:0 24px;box-shadow:0 1px 4px rgba(0,21,41,0.08);position:sticky;top:0;z-index:99;">';

    /* Left: breadcrumb */
    html += '<div id="proto-breadcrumb" style="font-size:14px;color:#666;"></div>';

    /* Right: tools */
    html += '<div style="display:flex;align-items:center;gap:8px;">';

    /* 通知 */
    html += '<div style="position:relative;cursor:pointer;padding:8px;border-radius:6px;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-07_通知中心.html\'" title="消息中心">';
    html += '<i class="ti ti-bell" style="font-size:20px;color:#666;"></i>';
    html += '<span id="shell-notify-badge-admin" style="position:absolute;top:4px;right:4px;background:#ff4d4f;color:#fff;font-size:10px;min-width:16px;height:16px;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0 4px;">3</span>';
    html += '</div>';

    /* 用户头像（下拉） */
    html += '<div id="avatar-wrap" style="position:relative;">';
    html += '<div id="avatar-trigger" style="display:flex;align-items:center;gap:8px;cursor:pointer;padding:6px 12px;border-radius:8px;transition:background 0.2s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.__toggleAvatarMenu()">';
    html += '<div style="width:28px;height:28px;border-radius:50%;background:linear-gradient(135deg,#1890ff,#36cfc9);display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;font-weight:600;">' + SHELL_CONFIG.currentUser.name.charAt(0) + '</div>';
    html += '<span style="font-size:13px;color:#333;">' + SHELL_CONFIG.currentUser.name + '</span>';
    html += '<span style="font-size:10px;color:#999;">▾</span>';
    html += '</div>';

    /* 下拉菜单 */
    html += '<div id="avatar-dropdown" style="display:none;position:absolute;top:calc(100% + 4px);right:0;min-width:180px;background:#fff;border-radius:10px;box-shadow:0 6px 24px rgba(0,0,0,0.12);padding:6px 0;z-index:200;">';
    html += '<div style="padding:12px 16px;border-bottom:1px solid #f0f0f0;">';
    html += '<div style="font-size:14px;font-weight:600;color:#1a1a2e;">' + SHELL_CONFIG.currentUser.name + '</div>';
    html += '<div style="font-size:12px;color:#8c8c8c;margin-top:2px;">' + SHELL_CONFIG.currentUser.role + '</div>';
    html += '</div>';
    /* 个人中心 */
    html += '<div class="dropdown-item" style="padding:10px 16px;cursor:pointer;font-size:13px;color:#333;display:flex;align-items:center;gap:8px;transition:background 0.15s;" onmouseover="this.style.background=\'#f5f5f5\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-08_个人中心.html\'">';
    html += '<i class="ti ti-user" style="font-size:16px;color:#8c8c8c;"></i>个人中心</div>';
    /* 切换到用户端 */
    html += '<div style="height:1px;background:#f0f0f0;margin:4px 0;"></div>';
    html += '<div class="dropdown-item" style="padding:10px 16px;cursor:pointer;font-size:13px;color:#1890ff;display:flex;align-items:center;gap:8px;transition:background 0.15s;" onmouseover="this.style.background=\'#e6f7ff\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-01_找方案.html\'">';
    html += '<i class="ti ti-switch-horizontal" style="font-size:16px;color:#1890ff;"></i>切换到用户端</div>';
    /* 退出登录 */
    html += '<div style="height:1px;background:#f0f0f0;margin:4px 0;"></div>';
    html += '<div class="dropdown-item" style="padding:10px 16px;cursor:pointer;font-size:13px;color:#ff4d4f;display:flex;align-items:center;gap:8px;transition:background 0.15s;" onmouseover="this.style.background=\'#fff2f0\'" onmouseout="this.style.background=\'transparent\'" onclick="window.location.href=\'PAGE-PC-00_登录页.html\'">';
    html += '<i class="ti ti-logout" style="font-size:16px;color:#ff4d4f;"></i>退出登录</div>';
    html += '</div>'; // end dropdown
    html += '</div>'; // end avatar-wrap

    html += '</div>'; // end right tools
    html += '</div>'; // end header

    /* Content */
    html += '<div id="proto-content" style="padding:20px 24px;min-height:calc(100vh - ' + headerHeight + 'px);">';
    html += '</div>';

    html += '</div>'; // end main
    html += '</div>'; // end shell

    /* 全局搜索浮层 */
    html += buildSearchOverlayHTML();


    document.body.innerHTML = html;

    // Move original content
    var contentArea = document.getElementById('proto-content');
    var tempDiv = document.createElement('div');
    tempDiv.innerHTML = originalContent;
    while (tempDiv.firstChild) {
      contentArea.appendChild(tempDiv.firstChild);
    }

    // Avatar dropdown toggle
    window.__toggleAvatarMenu = function() {
      var dd = document.getElementById('avatar-dropdown');
      dd.style.display = dd.style.display === 'none' ? 'block' : 'none';
    };
    document.addEventListener('click', function(e) {
      var wrap = document.getElementById('avatar-wrap');
      var dd = document.getElementById('avatar-dropdown');
      if (wrap && dd && !wrap.contains(e.target)) {
        dd.style.display = 'none';
      }
    });

    // Collapse toggle
    window.__toggleSider = function() {
      collapsed = !collapsed;
      var sider = document.getElementById('proto-sider');
      var main = document.getElementById('proto-main');
      var icon = document.getElementById('collapse-icon');
      var logoText = document.getElementById('logo-text');
      var labels = document.querySelectorAll('.menu-label');

      if (collapsed) {
        sider.style.width = collapsedWidth + 'px';
        main.style.marginLeft = collapsedWidth + 'px';
        icon.className = 'ti ti-chevron-right';
        if (logoText) logoText.style.display = 'none';
        labels.forEach(function(el) { el.style.display = 'none'; });
      } else {
        sider.style.width = siderWidth + 'px';
        main.style.marginLeft = siderWidth + 'px';
        icon.className = 'ti ti-chevron-left';
        if (logoText) logoText.style.display = '';
        labels.forEach(function(el) { el.style.display = ''; });
      }
    };
  }

  /* ========== 统一入口 ========== */
  function renderShell(currentPageKey) {
    var port = SHELL_CONFIG.pagePortMap[currentPageKey] || 'user';
    if (port === 'admin') {
      renderAdminShell(currentPageKey);
    } else {
      renderUserShell(currentPageKey);
    }
  }

  // Expose
  window.PROTO_SHELL = SHELL_CONFIG;
  window.PROTO_SHELL.renderShell = renderShell;
})();
