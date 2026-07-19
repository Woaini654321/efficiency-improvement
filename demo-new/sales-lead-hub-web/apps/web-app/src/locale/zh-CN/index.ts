import { zh_CN as zh_CN_of_common } from '@q-mono-x/locale'
import { zh_CN as zh_CN_of_qui } from '@q-ui/locale'
import { zh_CN as zh_CN_of_error_pages } from '@q-web-plugin/error-pages/src/locale'
import { zh_CN as zh_CN_of_antdv_pro } from '@q-web-plugin/antdv-pro'

export const zh_CN = {
  ...zh_CN_of_common, // 通用的中文包
  ...zh_CN_of_qui, // @q-ui 组件库中文包
  ...zh_CN_of_antdv_pro, // antdv 增强组件库中文包
  page: {
    ...zh_CN_of_error_pages.page, // 错误页中文包
    'login': '登录',
    'home': '首页',
    // ==== 用户端业务模块 ====
    'opportunity': {
      DEFAULT: '方案库',
      list: '查方案',
      detail: '方案详情',
      form: '发布/编辑方案'
    },
    'requirement': {
      DEFAULT: '需求广场',
      list: '提需求',
      detail: '需求详情',
      form: '发布/编辑需求'
    },
    'notification': {
      DEFAULT: '通知与订阅',
      list: '通知中心',
      preference: '通知偏好设置',
      announcement: '公告详情'
    },
    'profile': '个人中心',
    // ==== 运营中心 ====
    'operation': {
      DEFAULT: '运营中心',
      audit: { DEFAULT: '内容审核' },
      category: { DEFAULT: '分类维护' },
      dashboard: { DEFAULT: '数据看板' },
      sla: { DEFAULT: '需求时效监控' },
      log: { DEFAULT: '操作日志' },
      announce: { DEFAULT: '公告发布' },
      batch: { DEFAULT: '批量发布任务' }
    },
    // ==== 社区/情报/工具/任务 ====
    'discussion': {
      DEFAULT: '讨论区',
      list: '讨论区',
      detail: '讨论详情',
      post: '发帖'
    },
    'feedback': '吐槽墙',
    'intel': {
      DEFAULT: '情报中心',
      list: '情报中心',
      competitor: '竞品情报详情',
      industry: '行业情报详情',
      submit: '提交竞品情报'
    },
    'tool': '工具助手',
    'meeting': '会议任务管理',
    'task': '我的任务'
  },
  common: {
    action: '操作',
    detail: '详情',
    view: '查看',
    edit: '编辑',
    add: '新增',
    create: '新建',
    delete: '删除',
    remove: '移除',
    success: '操作成功',
    fail: '操作失败',
    cancel: '取消',
    confirm: '确认',
    ok: '确定',
    save: '保存',
    saveDraft: '保存草稿',
    publish: '发布',
    submit: '提交',
    search: '搜索',
    reset: '重置',
    filter: '筛选',
    back: '返回',
    close: '关闭',
    export: '导出',
    refresh: '刷新',
    more: '更多',
    all: '全部',
    noData: '暂无数据',
    loading: '加载中...',
    enabled: '启用',
    disabled: '停用',
    yes: '是',
    no: '否',
    allStatus: '全部状态',
    selectPlaceholder: '请选择',
    inputPlaceholder: '请输入',
    keyword: '关键词',
    keywordPlaceholder: '请输入关键词搜索',
    status: '状态',
    type: '类型',
    createdAt: '创建时间',
    updatedAt: '更新时间',
    publishedAt: '发布时间',
    publisher: '发布人',
    department: '部门',
    deleteConfirm: '确定删除该条记录吗？',
    batchDelete: '批量删除',
    like: '点赞',
    collect: '收藏',
    comment: '评论',
    viewCount: '浏览',
    share: '分享',
    follow: '关注'
  },
  // ==== 跨页共享枚举字典（一处定义，多页复用）====
  dict: {
    // 方案类型
    oppType: {
      product_info: '产品信息',
      solution: '解决方案',
      success_case: '成功案例'
    },
    // 方案状态
    oppStatus: {
      draft: '草稿',
      published: '已发布',
      archived: '已下架'
    },
    // 需求状态
    reqStatus: {
      Pending: '待响应',
      Collecting: '方案收集中',
      Adopted: '已采纳',
      Closed: '已关闭'
    },
    // 紧急程度
    urgency: {
      normal: '普通',
      urgent: '紧急',
      critical: '特急'
    },
    // SLA 响应状态
    slaStatus: {
      normal: '正常',
      warning: '临近截止',
      overdue: '已截止',
      responded: '已响应'
    },
    // 任务状态
    taskStatus: {
      pending: '待处理',
      processing: '处理中',
      done: '已完成',
      transferred: '已转交',
      cancelled: '已作废'
    },
    // 公告类型
    announceType: {
      notice: '通知',
      policy: '政策',
      activity: '活动',
      other: '其他'
    },
    // 公告状态
    announceStatus: {
      draft: '草稿',
      scheduled: '定时待发',
      published: '已发布',
      withdrawn: '已撤回'
    },
    // 优先级（公告）
    priority: {
      normal: '普通',
      important: '重要'
    },
    // 通知类型
    notifyType: {
      publish: '方案发布',
      response: '需求响应',
      adopt: '方案采纳',
      system: '系统通知',
      comment: '评论',
      reply: '回复',
      invite: '邀请回答',
      force_confirm: '强制确认',
      sla_remind: 'SLA催办',
      sla_escalate: 'SLA升级',
      archive: '内容下架',
      category_change: '分类变更',
      announcement: '公告推送',
      mention: '@提及',
      subscribe: '订阅更新'
    },
    // 通知渠道
    channel: {
      in_app: '站内信',
      feishu: '飞书',
      email: '邮箱'
    },
    // 情报类型
    intelType: {
      new_product: '新品发布',
      price_change: '价格变动',
      customer_case: '客户案例',
      other: '其他'
    },
    // 操作日志类型
    actionType: {
      publish: '发布',
      archive: '下架',
      delete: '删除',
      role_change: '角色变更',
      isolation_change: '隔离配置变更',
      category_change: '分类变更',
      login: '登录',
      sla_escalation: 'SLA升级'
    },
    result: {
      success: '成功',
      failure: '失败'
    },
    auditContentType: { opportunity: '方案信息', request: '方案需求' },
    auditStatus: {
      published: '已发布', archived: '已下架', pending: '待响应',
      collecting: '收集中', adopted: '已采纳', closed: '已关闭'
    }
  },
  // ==== opportunity 业务文案 ====
  opportunity: {
    add: '发布方案',
    editTitle: '编辑方案',
    publish: '发布方案',
    publishSuccess: '方案已发布',
    searchPlaceholder: '搜索方案标题/摘要',
    title: '方案标题',
    type: '方案类型',
    category: '分类标签',
    publisher: '发布人',
    dept: '发布部门',
    summary: '摘要',
    content: '方案详情',
    industry: '行业场景',
    keywords: '关键词标签',
    views: '浏览量',
    pinned: '置顶',
    attachments: '附件',
    metrics: '互动数据',
    archive: '下架',
    restore: '重新上架',
    copyAsNew: '复制为新方案',
    copyTip: '当前为「复制为新方案」，将以现有方案内容创建一条新草稿。',
    titlePlaceholder: '请输入方案标题（不超过100字）',
    categoryPlaceholder: '输入分类标签后回车，可添加多个',
    keywordsPlaceholder: '输入关键词后回车，可添加多个',
    summaryPlaceholder: '请输入方案摘要（不超过200字）',
    contentPlaceholder: '请输入方案详情内容',
    archiveConfirm: '确定下架该方案吗？下架后用户将无法浏览。'
  },
  requirement: {
    add: '发布需求',
    editTitle: '编辑需求',
    publish: '发布需求',
    publishSuccess: '需求已发布',
    searchPlaceholder: '搜索需求标题/描述',
    title: '需求标题',
    titlePlaceholder: '请输入需求标题（不超过100字）',
    urgency: '紧急程度',
    industry: '行业场景',
    publisher: '发布人',
    dept: '发布部门',
    category: '分类标签',
    categoryPlaceholder: '输入分类标签后回车，可添加多个',
    keywords: '关键词标签',
    keywordsPlaceholder: '输入关键词后回车，可添加多个',
    description: '需求描述',
    descriptionPlaceholder: '请输入需求详情描述',
    pinned: '置顶',
    responseCount: '方案响应数',
    baseInfo: '需求基本信息',
    slaStatus: '响应时效',
    visibility: '可见范围',
    visibilityType: { all: '全部可见', dept: '按部门', personnel: '指定人员' },
    metrics: '互动数据',
    invitedProductLines: '已邀请产品线',
    responses: '方案响应',
    adopted: '已采纳',
    criticalTip: '特急需求将立即触发 SLA 计时（首响时限 2 小时），请确认信息完整后再发布。'
  },
  home: {
    heroTitle: '销售商机互助平台',
    heroSubtitle: '一站式查方案、提需求、找情报、促成单',
    searchPlaceholder: '搜索方案、需求、讨论话题...',
    hotSearch: '热门搜索',
    stats: {
      solutionTotal: '方案总数',
      pendingRequests: '待响应需求',
      weekDiscussions: '本周讨论',
      activeUsers: '活跃用户'
    },
    quickActions: '快捷操作',
    action: {
      browse: '查方案',
      browseDesc: '浏览产品信息与解决方案',
      postReq: '提需求',
      postReqDesc: '发布商机需求求助',
      publish: '发布方案',
      publishDesc: '分享你的方案与案例',
      discuss: '发起讨论',
      discussDesc: '与同事交流经验'
    },
    taskBoard: '我的任务',
    pending: '待处理',
    overdue: '已逾期',
    viewAll: '查看全部',
    noTasks: '暂无待办任务',
    hotSolutions: '热门方案',
    browseAll: '浏览全部',
    subscribed: '已订阅',
    announcements: '公告',
    hotPosts: '讨论热帖',
    replies: '回复',
    topic: {
      opportunity: '商机讨论',
      solution: '方案互助',
      experience: '经验分享',
      industry: '行业动态',
      complaint: '吐槽'
    }
  },
  feedback: {
    title: '吐槽墙',
    subtitle: '匿名说出你的心声，让平台变得更好',
    postBtn: '我要吐槽',
    total: '共 {n} 条吐槽',
    tabHot: '热门',
    tabLatest: '最新',
    disclaimer: '所有吐槽均匿名发布，请文明发言，内容仅代表个人观点',
    anonTip: '吐槽将以匿名方式发布，他人无法看到你的真实身份',
    anonSelf: '匿名的我',
    formTitle: '标题',
    formTitlePlaceholder: '一句话说说你想吐槽什么',
    formContent: '内容',
    formContentPlaceholder: '详细说说你的想法或建议...',
    formRequired: '请填写标题和内容',
    postSuccess: '吐槽已发布'
  },
  tool: {
    title: '工具助手',
    subtitle: '为销售提效的常用小工具集合',
    enter: '进入',
    wip: '功能开发中',
    wipDesc: '该工具正在开发中，敬请期待',
    items: {
      quote: '报价计算器',
      quoteDesc: '快速估算方案报价与成本',
      compare: '竞品对比',
      compareDesc: '一键对比友商产品参数',
      template: '模板库',
      templateDesc: '常用方案与文档模板',
      email: '邮件模板',
      emailDesc: '客户跟进邮件一键生成',
      recommend: '方案推荐',
      recommendDesc: '按客户场景智能推荐方案',
      script: '话术生成',
      scriptDesc: '常见客户问题应答话术',
      writing: '智能撰写',
      writingDesc: 'AI 辅助撰写方案与文案'
    }
  },
  notification: {
    unread: '未读',
    read: '已读',
    searchPlaceholder: '搜索通知标题',
    type: '通知类型',
    readStatus: '阅读状态',
    gotoPreference: '通知偏好',
    gotoAnnouncement: '公告中心',
    title: '通知标题',
    triggerUser: '发起人',
    channel: '通知渠道',
    forceConfirm: '强制确认',
    markRead: '标记已读',
    preferenceTitle: '通知偏好设置',
    preferenceTip: '站内信为系统级通知，默认开启且不可关闭；飞书与邮箱渠道可按通知类型自由配置。',
    locked: '默认开启',
    notifyType: '通知类型',
    enableAll: '一键开启全部',
    disableAll: '一键关闭全部',
    restoreDefault: '恢复默认',
    saveSuccess: '通知偏好已保存',
    pinned: '置顶',
    backToList: '返回列表',
    publisher: '发布人',
    viewCount: '阅读量',
    announcementCenter: '公告中心'
  },
  profile: {
    employeeNo: '工号',
    subscriptionTip: '订阅感兴趣的分类，相关内容更新时将第一时间通知你。',
    currentSubscription: '当前订阅',
    cancelCollect: '取消收藏',
    deleted: '已删除',
    continueEdit: '继续编辑',
    adopted: '已采纳',
    relatedRequest: '关联需求',
    adoptedBy: '采纳人',
    viewOrigin: '查看原文',
    viewedAt: '浏览时间',
    stats: {
      collect: '我的收藏',
      comment: '我的评论',
      publish: '我的发布',
      solution: '我的方案',
      draft: '我的草稿',
      view: '浏览记录'
    },
    tab: {
      subscription: '订阅设置',
      collect: '我的收藏',
      publish: '我的发布',
      solution: '我的方案',
      comment: '我的评论',
      history: '浏览记录'
    },
    itemType: {
      opportunity: '方案',
      requirement: '需求'
    }
  },
  audit: {
    searchPlaceholder: '搜索标题/发布人',
    contentType: '内容类型',
    title: '内容标题',
    publisher: '发布人',
    pinned: '置顶',
    pin: '置顶',
    unpin: '取消置顶',
    archive: '下架',
    archiveConfirm: '确定下架该内容吗？下架后用户将无法浏览。',
    deleteConfirm: '确定删除该条内容吗？'
  },
  category: {
    addRoot: '新增分类',
    addChild: '新增子级',
    dictType: {
      solutionCategory: '方案分类', solutionType: '方案类型',
      requirementCategory: '需求分类', announceType: '公告类型'
    },
    name: '分类名称',
    nameEn: '英文名称',
    parent: '上级分类',
    parentPlaceholder: '不选则为顶级分类',
    sortOrder: '排序号',
    contentCount: '关联内容数',
    rootLevel: '顶级',
    deleteConfirm: '确定删除该分类吗？',
    deleteBlocked: '存在子分类或关联内容，禁止删除',
    editTitle: '编辑分类',
    createTitle: '新增分类',
    namePlaceholder: '请输入分类名称（不超过30字）',
    selectTip: '请从左侧选择分类查看详情'
  },
  log: {
    detailTitle: '操作日志详情',
    searchPlaceholder: '搜索操作人/操作对象',
    operator: '操作人',
    actionType: '操作类型',
    target: '操作对象',
    result: '操作结果',
    ip: 'IP 地址',
    userAgent: 'User-Agent',
    beforeSnapshot: '变更前',
    afterSnapshot: '变更后'
  },
  dashboard: {
    title: '运营数据看板', updatedAt: '数据更新时间',
    uv: '独立访客 UV', pv: '页面浏览量 PV',
    activeUsers: '活跃用户', weekPublish: '本周发布量',
    responseRate: '需求响应率', adoptRate: '方案采纳率', mom: '环比',
    hotContents: '热门内容 TOP5', categoryDist: '分类分布', pageHeat: '页面热度排行',
    range: { last7d: '近7天', last4w: '近4周', last12w: '近12周', last6m: '近半年' }
  },
  sla: {
    stat: { total: '总需求数', timelyRate: '及时响应率', responded: '已响应', maxOverdue: '最长超时' },
    detailTitle: '需求时效详情', title: '需求标题', publisher: '发布人', urgency: '紧急程度',
    createdAt: '发布时间', deadline: '截止时间', remaining: '剩余时间', escalation: '升级等级',
    responseCount: '已响应', responseUnit: '{n} 条', urge: '催办', urgeTitle: '催办提醒',
    urgeTargets: '通知对象', urgeMethods: '通知方式', urgeRemark: '附言', urgeSuccess: '催办已发送',
    target: { publisher: '发布人', supervisor: '直属主管', productLine: '产品线负责人' }
  },
  announce: {
    stat: { total: '公告总数', published: '已发布', draft: '草稿', totalViews: '总阅读量' },
    add: '新建公告', editTitle: '编辑公告', previewTitle: '公告预览', searchPlaceholder: '搜索公告标题',
    title: '公告标题', type: '类型', priority: '优先级', publisher: '发布人', views: '阅读量',
    pinned: '置顶', banner: '推送横幅', content: '正文', contentPlaceholder: '请输入公告正文',
    titlePlaceholder: '请输入公告标题（不超过100字）', withdraw: '下架',
    deleteConfirm: '确定删除该公告吗？', publishSuccess: '公告已发布'
  },
  batch: {
    title: '批量发布任务', step1: '会议信息', step2: '填写任务', step3: '发布完成',
    meeting: '选择会议', meetingDate: '会议日期', meetingName: '会议名称', recorder: '记录人',
    next: '下一步', prev: '上一步', sourceExist: '选择已有', sourceNew: '创建新会议',
    bulkSet: '批量设置', priority: '优先级', apply: '应用', deadline: '截止日期', executor: '执行人',
    taskNo: '任务 {n}', copyRow: '复制', descPlaceholder: '请输入任务描述', addTask: '添加任务',
    preview: '发布预览', publishN: '发布任务（{n}）', applied: '已应用到全部任务',
    meetingRequired: '请选择会议', meetingNameRequired: '请输入会议名称', taskRequired: '请至少填写一条任务描述',
    successTitle: '任务发布成功', successSub: '共发布 {n} 条任务', continue: '继续发布',
    col: { no: '序号', desc: '任务描述', priority: '优先级', deadline: '截止日期', executor: '执行人' }
  },
  discussion: {
    topicLabel: '话题分类',
    topic: { business: '商机讨论', solution: '方案互助', experience: '经验分享', industry: '行业动态', complaint: '吐槽' },
    searchPlaceholder: '搜索帖子标题/内容',
    post: '发帖',
    publishPost: '发布讨论',
    publishSuccess: '讨论已发布',
    title: '标题',
    hot: '热',
    author: '作者',
    replies: '回复',
    views: '浏览',
    content: '讨论内容',
    comments: '评论',
    titlePlaceholder: '请输入标题（不超过100字）',
    contentPlaceholder: '请输入讨论内容'
  },
  intel: {
    tabCompetitor: '竞品情报',
    tabIndustry: '行业情报',
    searchPlaceholder: '搜索情报标题/摘要',
    brand: '竞品品牌',
    product: '产品型号',
    intelType: '情报类型',
    source: '情报来源',
    submitter: '提交人',
    title: '情报标题',
    summary: '情报摘要',
    content: '情报内容',
    industry: '行业分类',
    submit: '提交竞品情报',
    submitAction: '提交情报',
    submitTip: '提交后将进入运营审核，审核通过后对外展示。',
    submitSuccess: '情报已提交，等待运营审核',
    sourcePlaceholder: '请输入情报来源',
    titlePlaceholder: '请输入情报标题（不超过100字）',
    contentPlaceholder: '请输入情报内容',
    industryDict: { trend: '行业趋势', automotive: '车载', policy: '法规政策', energy: '能源', industrial: '工业IoT', smartcity: '智慧城市' }
  },
  meeting: {
    addTitle: '新建会议任务',
    editTitle: '编辑会议任务',
    searchPlaceholder: '搜索会议名称/任务描述',
    taskId: '编号',
    meetingName: '会议名称',
    meetingDate: '会议日期',
    recorder: '记录人',
    taskDesc: '任务描述',
    priority: '优先级',
    deadline: '截止日期',
    assignees: '执行人',
    assigneesPlaceholder: '输入执行人后回车，可添加多个',
    urge: '催办',
    urgePlaceholder: '请输入催办说明',
    urgeSuccess: '催办已发送',
    cancel: '作废',
    cancelPlaceholder: '请输入作废原因'
  },
  task: {
    title: '我的任务',
    priorityFilter: '优先级筛选',
    transferFromLabel: '转交来源',
    recorder: '记录人',
    deadline: '截止日期',
    start: '开始处理',
    transfer: '转交',
    complete: '标记完成',
    waiting: '等待接收人处理',
    transferTo: '转交给',
    transferToPlaceholder: '请选择转交对象',
    transferReason: '转交原因',
    transferReasonPlaceholder: '请输入转交原因',
    transferSuccess: '任务已转交',
    completeRemark: '完成备注',
    completeRemarkPlaceholder: '请输入完成备注（可选）',
    completeSuccess: '任务已完成'
  }
}
