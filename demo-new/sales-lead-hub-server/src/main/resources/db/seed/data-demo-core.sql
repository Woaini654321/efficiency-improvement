-- =============================================================================
-- 销售商机互助平台 · 核心模块演示种子（切真后消除页面数据断层）
-- 覆盖：opportunity / opportunity_category / opportunity_request / request_category /
--       request_product_line / solution_response / interaction / announcement /
--       notification / subscription / view_log / audit_log / product_line_member
-- 依赖：先执行 schema.sql（含 opportunity/opportunity_request 的 is_pinned/sort_no 补列）
--       再执行 data.sql（分类 1..302 / 部门 10..2001 / 产品线 501..505 / 用户 9001-9004+10160 /
--       产品线成员 601-604）。本文件仅新增 96xxx 段业务演示数据。
-- 数据来源：apps/web-app/src/apis/{opportunity,requirement,announce,notification,
--       interaction,auditLog}/mocks/*.json（快照口径直取 mock 展示值）。
-- id 段固定分配（便于外键引用与幂等清理，绝不触碰 96000 以下既有数据与 97xxx 已有种子）：
--   opportunity 96001-96020 · opportunity_request 96101-96115 · solution_response 96201-96220 ·
--   interaction 96301-96360 · announcement 96401-96410 · notification 96501-96540 ·
--   subscription 96601-96610 · view_log 96701-96800 · audit_log 96901-96920 ·
--   product_line_member 补 96951-96955。
-- 审计列：create_by/update_by 统一 9001；create_time/update_time 取 mock 时间或合理时间。
-- 幂等：可重复执行（下方按依赖倒序先删 96xxx 段，再插）。
-- 计数一致性：见文件尾「一致性自检清单」。
-- =============================================================================
USE `sales_lead_hub_server`;
SET NAMES utf8mb4;

-- =============================================================================
-- 幂等 DELETE（依赖倒序；仅删本文件 96xxx id 段）
-- =============================================================================
DELETE FROM `interaction`           WHERE `id` BETWEEN 96301 AND 96360;
DELETE FROM `solution_response`     WHERE `id` BETWEEN 96201 AND 96220;
DELETE FROM `opportunity_category`  WHERE `opportunity_id` BETWEEN 96001 AND 96020;
DELETE FROM `request_category`      WHERE `request_id` BETWEEN 96101 AND 96115;
DELETE FROM `request_product_line`  WHERE `request_id` BETWEEN 96101 AND 96115;
DELETE FROM `notification`          WHERE `id` BETWEEN 96501 AND 96540;
DELETE FROM `subscription`          WHERE `id` BETWEEN 96601 AND 96610;
DELETE FROM `view_log`              WHERE `id` BETWEEN 96701 AND 96800;
DELETE FROM `audit_log`             WHERE `id` BETWEEN 96901 AND 96920;
DELETE FROM `announcement`          WHERE `id` BETWEEN 96401 AND 96410;
DELETE FROM `opportunity_request`   WHERE `id` BETWEEN 96101 AND 96115;
DELETE FROM `opportunity`           WHERE `id` BETWEEN 96001 AND 96020;
DELETE FROM `product_line_member`   WHERE `id` BETWEEN 96951 AND 96955;

-- =============================================================================
-- 1. opportunity 商机（12 条；publisher 轮流 9002/9003/9004/10160，姓名/部门快照对齐 data.sql）
--    多数 published；96009 archived(archived_by=9001)；96012 draft。
--    ⚠️ 热门 96001/96002/96003 的 like/collect/comment_count 已改写为 interaction 实际行数；
--       其余商机沿用 mock 展示计数（未种 interaction 行）。view_count 一律沿用 mock。
-- =============================================================================
INSERT INTO `opportunity`
 (`id`,`title`,`summary`,`content`,`type`,`attachments`,`status`,`publisher_id`,`publisher_name`,
  `department_id`,`publisher_dept_name`,`archived_by`,`is_pinned`,`sort_no`,`category_names`,
  `view_count`,`like_count`,`collect_count`,`comment_count`,`deleted`,`version`,`create_by`,`update_by`,`create_time`,`update_time`) VALUES
 (96001,'5G RedCap 模组 RG200U 产品选型方案','面向工业网关、视频监控场景的 5G RedCap 模组选型与对比，含功耗与成本分析。',
  '<h2>方案概述</h2><p>RG200U 基于 5G RedCap，兼顾成本与性能……</p>','solution',
  '[{"name":"RG200U选型手册.pdf","url":"/files/rg200u.pdf","size":2456789},{"name":"RedCap功耗对比.xlsx","url":"/files/redcap-power.xlsx","size":1345678},{"name":"硬件设计参考指南.pptx","url":"/files/rg200u-hw.pptx","size":5123456}]',
  'published',9002,'张伟',1001,'上海销售组',NULL,1,1,'["IoT 模组","5G 模组"]',
  1280,4,3,6,'N',0,9001,9001,'2026-06-20 09:12:00','2026-06-20 10:00:00'),
 (96002,'Cat.1 bis 模组在共享设备中的成功案例','某头部共享出行客户批量导入 EG912U，年出货 200 万片的落地实践。',
  '<h2>客户背景</h2><p>客户为国内共享两轮龙头……</p>','success_case','[]',
  'published',9003,'李娜',10,'华东大区',NULL,0,0,'["行业方案","Cat.1 模组"]',
  940,3,2,3,'N',0,9001,9001,'2026-06-18 14:30:00','2026-06-18 15:00:00'),
 (96003,'车载 4G+C-V2X 通信模组 AG59x 产品信息','AG59x 系列面向智能座舱与 T-Box，支持 C-V2X 直连通信。',
  '<h2>产品参数</h2><p>支持 LTE Cat.6/C-V2X……</p>','product_info',
  '[{"name":"AG59x规格书.pdf","url":"/files/ag59x.pdf","size":3987654}]',
  'published',9004,'王强',2001,'深圳销售组',NULL,0,0,'["车载方案"]',
  1560,2,2,3,'N',0,9001,9001,'2026-06-15 11:20:00','2026-06-15 13:00:00'),
 (96004,'NB-IoT 智慧水表整体解决方案','BC660K-GL 低功耗方案，配套 DTU 与云平台对接指引。',
  '<h2>系统架构</h2><p>终端-网络-平台三层架构……</p>','solution','[]',
  'published',10160,'atom.ye',1001,'上海销售组',NULL,0,0,'["行业方案","NB-IoT 模组"]',
  720,40,22,8,'N',0,9001,9001,'2026-06-12 08:45:00','2026-06-12 09:30:00'),
 (96005,'Wi-Fi 6 + BLE 二合一模组 FCU630 产品信息','适用于智能家居网关，支持 Wi-Fi 6 与 BLE 5.2 并发。',
  '<h2>产品亮点</h2><p>双频并发，低时延……</p>','product_info','[]',
  'published',9002,'张伟',1001,'上海销售组',NULL,0,0,'["IoT 模组","智慧城市"]',
  510,28,15,5,'N',0,9001,9001,'2026-06-10 16:00:00','2026-06-10 16:30:00'),
 (96006,'天线设计——5G Sub-6G 高增益贴片天线方案','面向 CPE 的高增益天线布局与调试经验分享。',
  '<h2>天线布局</h2><p>贴片阵列设计……</p>','solution',
  '[{"name":"天线仿真报告.pptx","url":"/files/ant.pptx","size":1567890}]',
  'published',9003,'李娜',10,'华东大区',NULL,0,0,'["天线产品"]',
  388,19,9,3,'N',0,9001,9001,'2026-06-08 10:10:00','2026-06-08 11:00:00'),
 (96007,'智慧城市 LoRa 网关部署成功案例','某新区智慧路灯项目 LoRa 网关规模化部署复盘。',
  '<h2>项目概况</h2><p>覆盖 2000+ 路灯节点……</p>','success_case','[]',
  'published',9004,'王强',2001,'深圳销售组',NULL,0,0,'["行业方案","智慧城市"]',
  655,44,27,11,'N',0,9001,9001,'2026-06-05 13:40:00','2026-06-05 14:00:00'),
 (96008,'GNSS 高精度定位模组 LC29H 产品信息','支持 RTK 厘米级定位，适用于无人机与机器人。',
  '<h2>定位性能</h2><p>双频 RTK，收敛快……</p>','product_info','[]',
  'published',10160,'atom.ye',1001,'上海销售组',NULL,0,0,'["IoT 模组"]',
  1120,73,41,18,'N',0,9001,9001,'2026-06-02 09:00:00','2026-06-02 09:20:00'),
 (96009,'工业路由 5G CPE 参考设计方案','基于 RM520N 的工业 CPE 整机参考设计与散热建议。',
  '<h2>整机设计</h2><p>含结构与散热……</p>','solution','[]',
  'archived',9002,'张伟',1001,'上海销售组',9001,0,0,'["IoT 模组","5G 模组"]',
  233,12,6,2,'N',0,9001,9001,'2026-05-28 15:20:00','2026-05-28 16:00:00'),
 (96010,'智能POS Cat.1 模组 EG915U 产品信息','面向智能POS与金融支付终端的 Cat.1 模组。',
  '<h2>安全特性</h2><p>支持国密算法……</p>','product_info','[]',
  'published',9003,'李娜',10,'华东大区',NULL,0,0,'["Cat.1 模组"]',
  470,25,13,4,'N',0,9001,9001,'2026-05-25 11:00:00','2026-05-25 11:30:00'),
 (96011,'农业物联网 NB-IoT 土壤墒情监测方案','低功耗土壤监测终端方案，含休眠策略与电池续航测算。',
  '<h2>续航测算</h2><p>典型 5 年续航……</p>','solution','[]',
  'published',9004,'王强',2001,'深圳销售组',NULL,0,0,'["行业方案","NB-IoT 模组"]',
  289,16,8,1,'N',0,9001,9001,'2026-05-20 10:30:00','2026-05-20 11:00:00'),
 (96012,'车规级以太网 T1 模组产品信息（草稿）','面向域控制器的车规以太网通信模组，产品资料整理中。',
  '<h2>待完善</h2><p>规格整理中……</p>','product_info','[]',
  'draft',10160,'atom.ye',1001,'上海销售组',NULL,0,0,'["车载方案"]',
  0,0,0,0,'N',0,9001,9001,'2026-06-22 17:00:00','2026-06-22 17:00:00');

-- =============================================================================
-- 2. opportunity_category 关联（仅 published 商机；分类名反查 data.sql category id；名对不上跳过）
--    96009(archived)/96012(draft) 不建关联；category_names 快照已在上表保留。
-- =============================================================================
INSERT INTO `opportunity_category` (`opportunity_id`,`category_id`) VALUES
 (96001,1),(96001,101),
 (96002,3),(96002,103),
 (96003,2),
 (96004,3),(96004,102),
 (96005,1),(96005,301),
 (96006,4),
 (96007,3),(96007,301),
 (96008,1),
 (96010,103),
 (96011,3),(96011,102);

-- =============================================================================
-- 3. opportunity_request 商机需求（12 条；urgency 三档齐全；status 覆盖 Pending/Collecting/Adopted/Closed）
--    可见性：大多 all；96102=dept(含 1001)；96104=personnel(含 10160)。
--    SLA 演示：96106=NOW()-1h(剩余)；96109=NOW()-3d & 96111=NOW()-5d(已超时)。
--    response_count 与 solution_response 行数一致；采纳双向：96101↔96201、96107↔96204。
--    热门 96101/96105 的 like/collect/comment_count = interaction 实际行数；其余沿用 mock，comment_count=0(未种评论)。
-- =============================================================================
INSERT INTO `opportunity_request`
 (`id`,`title`,`description`,`industry`,`urgency`,`status`,`adopted_response_id`,`publisher_id`,`publisher_name`,
  `department_id`,`publisher_dept_name`,`visibility_scope`,`visibility_values`,`is_pinned`,`sort_no`,
  `invited_product_line_names`,`category_names`,`sla_status`,`escalation_level`,
  `view_count`,`response_count`,`like_count`,`collect_count`,`comment_count`,`deleted`,`version`,
  `create_by`,`update_by`,`create_time`,`update_time`) VALUES
 (96101,'求一款支持 -40~85℃ 宽温的 Cat.1 模组用于户外表计',
  '<h2>需求背景</h2><p>户外燃气/水表项目，需支持宽温工作、低功耗待机，单价目标可控。</p><p>期望提供选型建议与参考功耗数据。</p>',
  '智慧公用事业','critical','Adopted',96201,9002,'张伟',1001,'上海销售组','all',NULL,1,1,
  '["无线模组产品线","低功耗产品线"]','["Cat.1 模组","低功耗"]','responded','L0',
  486,3,3,2,4,'N',0,9001,9001,'2026-07-01 09:12:00','2026-07-03 09:30:00'),
 (96102,'车载 T-Box 需要 5G+C-V2X 二合一方案，含天线布局建议',
  '<h2>需求描述</h2><p>某车厂前装 T-Box 项目，要求 5G 主通信 + C-V2X 直连，需整体通信+天线方案。</p>',
  '汽车电子','urgent','Collecting',NULL,9003,'李娜',10,'华东大区','dept','[1001]',0,0,
  '["车载产品线","天线产品线"]','["车载方案","5G 模组"]','warning','L1',
  312,2,27,11,0,'N',0,9001,9001,'2026-07-08 14:30:00','2026-07-11 15:20:00'),
 (96103,'智慧农业土壤墒情终端，求 5 年续航的 NB-IoT 方案',
  '<h2>需求描述</h2><p>农业物联网项目，太阳能+电池供电，要求典型工况 5 年续航，需休眠策略建议。</p>',
  '智慧农业','normal','Pending',NULL,9004,'王强',2001,'深圳销售组','all',NULL,0,0,
  '[]','["NB-IoT 模组"]','overdue','L1',
  98,0,6,3,0,'N',0,9001,9001,'2026-07-15 08:45:00','2026-07-15 08:45:00'),
 (96104,'智能POS 求国密支持的 Cat.1 模组（已关闭）',
  '<h2>需求描述</h2><p>金融支付终端，需支持国密算法与安全存储，项目已确定其他方案，需求关闭归档。</p>',
  '金融支付','normal','Closed',NULL,10160,'atom.ye',1001,'上海销售组','personnel','[10160]',0,0,
  '["无线模组产品线"]','["Cat.1 模组","安全"]','responded','L0',
  205,0,9,4,0,'N',0,9001,9001,'2026-06-25 11:00:00','2026-06-28 16:00:00'),
 (96105,'工业网关急需 5G RedCap 低成本方案（特急）',
  '<h2>需求描述</h2><p>工业网关客户对成本敏感，希望用 RedCap 替代完整 5G，需选型与成本对比。</p>',
  '工业互联网','critical','Collecting',NULL,9002,'张伟',1001,'上海销售组','all',NULL,1,1,
  '["无线模组产品线"]','["5G 模组","5G RedCap"]','overdue','L2',
  420,1,2,2,3,'N',0,9001,9001,'2026-07-12 10:20:00','2026-07-14 09:10:00'),
 (96106,'共享设备批量导入，求高性价比 Cat.1 bis 模组',
  '<h2>需求描述</h2><p>共享出行客户年出货预计 150 万片，需高性价比 Cat.1 bis 模组及供货保障。</p>',
  '共享出行','urgent','Pending',NULL,9003,'李娜',10,'华东大区','all',NULL,0,0,
  '["无线模组产品线"]','["Cat.1 模组"]','normal','L0',
  156,0,12,7,0,'N',0,9001,9001,DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR)),
 (96107,'无人机 RTK 高精度定位模组选型',
  '<h2>需求描述</h2><p>工业级无人机项目，需厘米级 RTK 定位、快速收敛，适配现有飞控接口。</p>',
  '低空经济','urgent','Adopted',96204,9004,'王强',2001,'深圳销售组','all',NULL,0,0,
  '["定位产品线"]','["GNSS 定位模组","高精度定位"]','responded','L0',
  278,2,21,9,0,'N',0,9001,9001,'2026-07-03 09:00:00','2026-07-05 14:30:00'),
 (96108,'智能家居网关求 Wi-Fi 6 + BLE 二合一模组',
  '<h2>需求描述</h2><p>智能家居网关新品，需 Wi-Fi 6 与 BLE 5.2 并发，低时延，兼顾成本。</p>',
  '智能家居','normal','Collecting',NULL,10160,'atom.ye',1001,'上海销售组','all',NULL,0,0,
  '["无线模组产品线"]','["Wi-Fi","短距"]','normal','L0',
  134,1,14,6,0,'N',0,9001,9001,'2026-07-11 16:00:00','2026-07-13 11:20:00'),
 (96109,'智慧城市路灯 LoRa 网关规模化部署方案咨询（特急）',
  '<h2>需求描述</h2><p>新区智慧路灯项目，2000+ 节点，需 LoRa 网关选型与组网建议。</p>',
  '智慧城市','critical','Pending',NULL,9002,'张伟',1001,'上海销售组','all',NULL,0,0,
  '["无线模组产品线"]','["LoRa","智慧城市"]','overdue','L3',
  189,0,18,8,0,'N',0,9001,9001,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY)),
 (96110,'工业路由 5G CPE 参考设计求散热优化建议',
  '<h2>需求描述</h2><p>工业 CPE 整机项目，RM520N 方案发热偏高，需结构与散热优化建议。</p>',
  '工业互联网','urgent','Closed',NULL,9003,'李娜',10,'华东大区','all',NULL,0,0,
  '["无线模组产品线"]','["5G 模组"]','responded','L0',
  143,0,8,3,0,'N',0,9001,9001,'2026-06-18 15:20:00','2026-06-20 15:20:00'),
 (96111,'医疗监护设备求低功耗蓝牙模组，需医疗合规支持',
  '<h2>需求描述</h2><p>便携监护设备，需 BLE 低功耗模组，并提供医疗认证与合规支持资料。</p>',
  '医疗健康','normal','Pending',NULL,9004,'王强',2001,'深圳销售组','all',NULL,0,0,
  '[]','[]','overdue','L2',
  67,0,4,2,0,'N',0,9001,9001,DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY)),
 (96112,'车规以太网 T1 模组求域控项目选型资料',
  '<h2>需求描述</h2><p>域控制器项目，需车规级以太网 T1 通信模组，规格资料整理中，先征集方向。</p>',
  '汽车电子','normal','Collecting',NULL,10160,'atom.ye',1001,'上海销售组','all',NULL,0,0,
  '["车载产品线"]','["车载方案"]','normal','L0',
  52,1,3,1,0,'N',0,9001,9001,'2026-07-17 17:00:00','2026-07-18 10:00:00');

-- =============================================================================
-- 4. request_category 关联（分类名反查 data.sql；名对不上跳过。96108/96111 无匹配分类不建行）
-- =============================================================================
INSERT INTO `request_category` (`request_id`,`category_id`) VALUES
 (96101,103),
 (96102,2),(96102,101),
 (96103,102),
 (96104,103),
 (96105,101),(96105,10101),
 (96106,103),
 (96107,104),
 (96109,301),
 (96110,101),
 (96112,2);

-- =============================================================================
-- 5. request_product_line 关联（invited_product_line_names 快照对应的真实产品线 id）
-- =============================================================================
INSERT INTO `request_product_line` (`request_id`,`product_line_id`) VALUES
 (96101,501),(96101,502),
 (96102,503),(96102,504),
 (96104,501),
 (96105,501),
 (96106,501),
 (96107,505),
 (96108,501),
 (96109,501),
 (96110,501),
 (96112,503);

-- =============================================================================
-- 6. solution_response 方案响应（10 条；Collecting/Adopted 需求各配 1~3 条）
--    responder 轮流 9002-9004；Adopted 需求各 1 条 is_adopted=1（与 request.adopted_response_id 双向一致）。
-- =============================================================================
INSERT INTO `solution_response`
 (`id`,`request_id`,`content`,`attachments`,`responder_id`,`responder_name`,`department_id`,`responder_dept_name`,
  `is_adopted`,`email_recipients`,`custom_email_recipients`,`feishu_sync`,`version`,`create_by`,`update_by`,`create_time`,`update_time`) VALUES
 (96201,96101,'推荐 EG912U-GL，宽温 -40~85℃，PSM 待机 < 3uA，已在户外表计批量落地。附选型手册。',
  '["EG912U-GL选型手册.pdf", "户外表计功耗测试报告.xlsx"]',
  9003,'李娜',10,'华东大区',1,'[]','[]',0,0,9001,9001,'2026-07-02 10:20:00','2026-07-03 09:30:00'),
 (96202,96101,'可考虑 BC660K-GL（NB-IoT），若网络覆盖满足，功耗更优。',
  '["BC660K-GL规格书.pdf"]',
  9004,'王强',2001,'深圳销售组',0,'[]','[]',0,0,9001,9001,'2026-07-02 14:05:00','2026-07-02 14:05:00'),
 (96203,96101,'补充 EG915U-EU 欧洲频段版本，供出口项目参考。',
  '[]',9002,'张伟',1001,'上海销售组',0,'[]','[]',0,0,9001,9001,'2026-07-03 09:30:00','2026-07-03 09:30:00'),
 (96204,96107,'LC29H 双频 RTK，收敛快、体积小，已在无人机批量验证，推荐。',
  '["LC29H规格书.pdf", "RTK收敛测试报告.pdf"]',
  9003,'李娜',10,'华东大区',1,'[]','[]',0,0,9001,9001,'2026-07-05 10:00:00','2026-07-05 14:30:00'),
 (96205,96107,'如需更高动态性能可评估 LG69T，供参考。',
  '[]',9004,'王强',2001,'深圳销售组',0,'[]','[]',0,0,9001,9001,'2026-07-05 14:30:00','2026-07-05 14:30:00'),
 (96206,96102,'AG59x 系列支持 5G + C-V2X，附参考设计与散热建议。',
  '["AG59x参考设计.pdf"]',
  9004,'王强',2001,'深圳销售组',0,'[]','[]',0,0,9001,9001,'2026-07-10 11:00:00','2026-07-10 11:00:00'),
 (96207,96102,'配套 5G Sub-6G + V2X 组合天线布局，附仿真报告。',
  '["天线布局仿真报告.pdf", "天线选型清单.xlsx"]',
  9002,'张伟',1001,'上海销售组',0,'[]','[]',0,0,9001,9001,'2026-07-11 15:20:00','2026-07-11 15:20:00'),
 (96208,96105,'RG200U-CN 基于 RedCap，成本较完整 5G 下降约 30%，附对比表。',
  '["RedCap成本对比表.xlsx"]',
  9002,'张伟',1001,'上海销售组',0,'[]','[]',0,0,9001,9001,'2026-07-14 09:10:00','2026-07-14 09:10:00'),
 (96209,96108,'FCU630 支持 Wi-Fi 6 + BLE 5.2 双频并发，附方案文档。',
  '["FCU630方案文档.pdf"]',
  9004,'王强',2001,'深圳销售组',0,'[]','[]',0,0,9001,9001,'2026-07-13 11:20:00','2026-07-13 11:20:00'),
 (96210,96112,'车规以太网 T1 模组规格草案整理中，可先提供既有 100BASE-T1 参考资料对齐方向。',
  '[]',9003,'李娜',10,'华东大区',0,'[]','[]',0,0,9001,9001,'2026-07-18 10:00:00','2026-07-18 10:00:00');

-- =============================================================================
-- 7. interaction 互动（44 行；5 个热门 target 各配两级评论 + like/collect）
--    ⚠️ INSERT 列清单绝不含生成列 reaction_uk；like/collect 的 (user,target,type) 天然唯一。
--    user_name 快照与 user_id 一致（9002 张伟 / 9003 李娜 / 9004 王强 / 10160 atom.ye）。
--    parent_comment_id 指向同 target 的一级评论。
-- =============================================================================
INSERT INTO `interaction`
 (`id`,`user_id`,`user_name`,`target_type`,`target_id`,`type`,`content`,`parent_comment_id`,`content_deleted`,`create_time`) VALUES
 -- ----- Opportunity 96001（评论 6 / 赞 4 / 藏 3）-----
 (96301,9002,'张伟','Opportunity',96001,'comment','这份 RedCap 选型对比很实用，功耗数据是实测还是规格书标称？',NULL,0,'2026-06-21 09:30:00'),
 (96302,9003,'李娜','Opportunity',96001,'comment','功耗为实验室实测，测试条件见附件手册第 12 页。',96301,0,'2026-06-21 10:05:00'),
 (96303,9004,'王强','Opportunity',96001,'comment','补充一下，车载场景高温下功耗会再上浮约 8%。',96301,0,'2026-06-21 11:20:00'),
 (96304,10160,'atom.ye','Opportunity',96001,'comment','客户已经在评估同类方案，能否提供一份简版的商务报价参考？',NULL,0,'2026-06-21 14:12:00'),
 (96305,9002,'张伟','Opportunity',96001,'comment','报价我私信你，量级不同价格差异较大。',96304,0,'2026-06-21 15:00:00'),
 (96306,9004,'王强','Opportunity',96001,'comment','方案写得很清晰，收藏了，后面网关项目会参考。',NULL,0,'2026-06-22 08:40:00'),
 (96307,9003,'李娜','Opportunity',96001,'like',NULL,NULL,0,'2026-06-21 10:06:00'),
 (96308,9004,'王强','Opportunity',96001,'like',NULL,NULL,0,'2026-06-21 11:21:00'),
 (96309,10160,'atom.ye','Opportunity',96001,'like',NULL,NULL,0,'2026-06-21 14:13:00'),
 (96310,9002,'张伟','Opportunity',96001,'like',NULL,NULL,0,'2026-06-22 08:41:00'),
 (96311,9003,'李娜','Opportunity',96001,'collect',NULL,NULL,0,'2026-06-21 10:07:00'),
 (96312,10160,'atom.ye','Opportunity',96001,'collect',NULL,NULL,0,'2026-06-21 14:14:00'),
 (96313,9004,'王强','Opportunity',96001,'collect',NULL,NULL,0,'2026-06-22 08:42:00'),
 -- ----- Opportunity 96002（评论 3 / 赞 3 / 藏 2）-----
 (96314,9003,'李娜','Opportunity',96002,'comment','EC200A 在这个电表项目的低功耗表现如何？客户对待机电流很敏感。',NULL,0,'2026-06-24 09:20:00'),
 (96315,9002,'张伟','Opportunity',96002,'comment','PSM 模式下待机电流可做到微安级，具体数值以实测报告为准。',96314,0,'2026-06-24 10:15:00'),
 (96316,9004,'王强','Opportunity',96002,'comment','补充：eDRX 周期建议按抄表频率配置，能进一步省电。',96314,0,'2026-06-24 11:00:00'),
 (96317,9002,'张伟','Opportunity',96002,'like',NULL,NULL,0,'2026-06-24 10:16:00'),
 (96318,9003,'李娜','Opportunity',96002,'like',NULL,NULL,0,'2026-06-24 09:21:00'),
 (96319,10160,'atom.ye','Opportunity',96002,'like',NULL,NULL,0,'2026-06-24 12:00:00'),
 (96320,9004,'王强','Opportunity',96002,'collect',NULL,NULL,0,'2026-06-24 11:01:00'),
 (96321,9002,'张伟','Opportunity',96002,'collect',NULL,NULL,0,'2026-06-24 10:17:00'),
 -- ----- Opportunity 96003（评论 3 / 赞 2 / 藏 2）-----
 (96322,9004,'王强','Opportunity',96003,'comment','RG500Q 在这个车载网关项目有没有过车规认证？客户要看 AEC-Q100。',NULL,0,'2026-06-26 10:40:00'),
 (96323,9002,'张伟','Opportunity',96003,'comment','车规版本已过 AEC-Q100 Grade 2，认证报告可提供给客户。',96322,0,'2026-06-26 11:25:00'),
 (96324,9003,'李娜','Opportunity',96003,'comment','整理得很全面，已收藏，下周车厂交流会用到。',NULL,0,'2026-06-27 08:50:00'),
 (96325,9002,'张伟','Opportunity',96003,'like',NULL,NULL,0,'2026-06-26 11:26:00'),
 (96326,10160,'atom.ye','Opportunity',96003,'like',NULL,NULL,0,'2026-06-27 09:00:00'),
 (96327,9003,'李娜','Opportunity',96003,'collect',NULL,NULL,0,'2026-06-27 08:51:00'),
 (96328,9004,'王强','Opportunity',96003,'collect',NULL,NULL,0,'2026-06-26 10:41:00'),
 -- ----- Request 96101（评论 4 / 赞 3 / 藏 2）-----
 (96329,9004,'王强','Request',96101,'comment','这个需求我们产品线可以响应，天线部分有现成参考设计。',NULL,0,'2026-07-01 16:30:00'),
 (96330,9003,'李娜','Request',96101,'comment','太好了，麻烦尽快提交方案，客户催得比较急。',96329,0,'2026-07-01 17:10:00'),
 (96331,9002,'张伟','Request',96101,'comment','请问对定位精度有硬性要求吗？RTK 和普通 GNSS 成本差很多。',NULL,0,'2026-07-02 09:15:00'),
 (96332,10160,'atom.ye','Request',96101,'comment','亚米级即可，暂时不需要 RTK。',96331,0,'2026-07-02 10:00:00'),
 (96333,9002,'张伟','Request',96101,'like',NULL,NULL,0,'2026-07-01 16:31:00'),
 (96334,9003,'李娜','Request',96101,'like',NULL,NULL,0,'2026-07-01 17:11:00'),
 (96335,9004,'王强','Request',96101,'like',NULL,NULL,0,'2026-07-02 09:16:00'),
 (96336,10160,'atom.ye','Request',96101,'collect',NULL,NULL,0,'2026-07-02 10:01:00'),
 (96337,9002,'张伟','Request',96101,'collect',NULL,NULL,0,'2026-07-01 16:32:00'),
 -- ----- Request 96105（评论 3 / 赞 2 / 藏 2）-----
 (96338,9003,'李娜','Request',96105,'comment','关注这个 RedCap 需求，成本对比很关键，期待方案。',NULL,0,'2026-07-12 13:50:00'),
 (96339,9004,'王强','Request',96105,'comment','RG200U-CN 成本较完整 5G 下降约 30%，稍后附对比表。',96338,0,'2026-07-12 14:30:00'),
 (96340,10160,'atom.ye','Request',96105,'comment','客户催得急，麻烦产品线尽快响应。',NULL,0,'2026-07-13 09:00:00'),
 (96341,9002,'张伟','Request',96105,'like',NULL,NULL,0,'2026-07-12 13:51:00'),
 (96342,9004,'王强','Request',96105,'like',NULL,NULL,0,'2026-07-12 14:31:00'),
 (96343,9003,'李娜','Request',96105,'collect',NULL,NULL,0,'2026-07-12 13:52:00'),
 (96344,10160,'atom.ye','Request',96105,'collect',NULL,NULL,0,'2026-07-13 09:01:00');

-- =============================================================================
-- 8. announcement 公告（8 条；2 置顶 published+banner；5 published；1 draft；1 archived；publisher 9001）
-- =============================================================================
INSERT INTO `announcement`
 (`id`,`title`,`content`,`type`,`priority`,`publisher_id`,`publisher_name`,`is_pinned`,`status`,
  `view_count`,`banner_enabled`,`version`,`published_at`,`create_by`,`update_by`,`create_time`,`update_time`) VALUES
 (96401,'关于平台商机响应闭环率考核办法的通知','<p>为提升需求响应闭环率，现发布考核办法……</p>','policy','high',9001,'运营管理员',1,'published',
  2340,1,0,'2026-07-10 10:00:00',9001,9001,'2026-07-10 09:00:00','2026-07-10 10:00:00'),
 (96402,'平台SLA首响时限规则说明','<p>特急2h/紧急4h/普通24h……</p>','notice','high',9001,'运营管理员',1,'published',
  3020,1,0,'2026-07-08 09:00:00',9001,9001,'2026-07-08 08:30:00','2026-07-08 09:00:00'),
 (96403,'7月产品线技术分享会报名开启','<p>本月技术分享会主题为 5G RedCap……</p>','activity','normal',9001,'运营管理员',0,'published',
  1180,0,0,'2026-07-09 15:00:00',9001,9001,'2026-07-09 14:00:00','2026-07-09 15:00:00'),
 (96404,'新版方案库分类标签调整说明','<p>分类标签体系升级……</p>','policy','normal',9001,'运营管理员',0,'published',
  860,0,0,'2026-07-06 11:00:00',9001,9001,'2026-07-06 10:00:00','2026-07-06 11:00:00'),
 (96405,'关于规范需求发布内容的倡议','<p>请发布需求时明确场景与紧急程度……</p>','other','normal',9001,'运营管理员',0,'published',
  420,0,0,'2026-07-03 10:00:00',9001,9001,'2026-07-03 09:30:00','2026-07-03 10:00:00'),
 (96406,'优质方案作者季度评选启动','<p>季度评选正式启动……</p>','activity','normal',9001,'运营管理员',0,'published',
  1560,0,0,'2026-07-01 10:00:00',9001,9001,'2026-07-01 09:00:00','2026-07-01 10:00:00'),
 (96407,'关于清理超期未响应需求的公告','<p>草稿整理中……</p>','notice','normal',9001,'运营管理员',0,'draft',
  0,0,0,NULL,9001,9001,'2026-07-15 11:20:00','2026-07-15 11:20:00'),
 (96408,'平台维护升级停机通知','<p>原定停机窗口已过期，本公告不再展示……</p>','notice','high',9001,'运营管理员',0,'archived',
  540,0,0,'2026-07-04 18:30:00',9001,9001,'2026-07-04 18:00:00','2026-07-04 18:30:00');

-- =============================================================================
-- 9. notification 通知（15 条；user_id 全部 10160；类型覆盖 response/adopt/publish/comment/
--    announcement/sla_remind/force_confirm/system；混合已读未读；96505 强制确认且未确认）
--    ⚠️ mock 的 mention/subscribe 不在 schema 枚举，已分别映射为 comment/publish；
--       target_id 指向本文件种下的商机/需求/公告 id。
-- =============================================================================
INSERT INTO `notification`
 (`id`,`user_id`,`batch_id`,`target_type`,`target_id`,`type`,`channel`,`title`,`trigger_user_name`,
  `is_read`,`is_force_confirm`,`confirm_time`,`create_time`) VALUES
 (96501,10160,NULL,'requirement',96101,'response','in_app','您发布的需求「宽温 Cat.1 模组用于户外表计」收到了新的方案响应','李娜',0,0,NULL,'2026-07-18 09:20:00'),
 (96502,10160,NULL,'requirement',96107,'adopt','in_app','您提交的方案已被采纳为「无人机 RTK 定位模组选型」的最佳方案','李娜',0,0,NULL,'2026-07-18 08:45:00'),
 (96503,10160,NULL,'opportunity',96001,'publish','feishu','您订阅的「5G 模组」分类发布了新方案「RG200U 产品选型方案」','张伟',0,0,NULL,'2026-07-17 16:30:00'),
 (96504,10160,NULL,'opportunity',96003,'comment','in_app','王强 在方案「AG59x 产品信息」的评论中 @ 了您','王强',0,0,NULL,'2026-07-17 14:10:00'),
 (96505,10160,NULL,'announcement',96401,'force_confirm','email','【重要】商机响应闭环率考核办法，请确认知悉后继续使用平台','运营管理员',0,1,NULL,'2026-07-17 10:00:00'),
 (96506,10160,NULL,'opportunity',96007,'publish','in_app','您订阅的「行业方案」分类有新的成功案例更新','孙丽',1,0,NULL,'2026-07-16 18:20:00'),
 (96507,10160,NULL,'system',NULL,'system','in_app','您的账号已成功绑定飞书，后续将通过飞书接收重要通知','系统通知',1,0,NULL,'2026-07-16 09:00:00'),
 (96508,10160,NULL,'requirement',96105,'response','feishu','您订阅的需求「工业网关 RedCap 方案」收到了新方案','吴敏',1,0,NULL,'2026-07-15 15:40:00'),
 (96509,10160,NULL,'opportunity',96005,'publish','in_app','您关注的发布人 陈涛 发布了新方案「FCU630 产品信息」','陈涛',1,0,NULL,'2026-07-15 11:05:00'),
 (96510,10160,NULL,'requirement',96109,'adopt','email','您响应的需求「LoRa 网关部署」方案未被采纳，感谢参与','孙丽',1,0,NULL,'2026-07-14 17:30:00'),
 (96511,10160,NULL,'opportunity',96003,'comment','in_app','周杰 在方案「AG59x 产品信息」的评论中回复并 @ 了您','周杰',1,0,NULL,'2026-07-14 10:15:00'),
 (96512,10160,NULL,'announcement',96402,'system','in_app','平台将于本周六 22:00-24:00 进行系统维护，届时可能短暂不可用','系统通知',1,0,NULL,'2026-07-13 09:00:00'),
 (96513,10160,NULL,'announcement',96401,'announcement','in_app','您有一条新公告：关于平台商机响应闭环率考核办法的通知','运营管理员',1,0,NULL,'2026-07-13 08:00:00'),
 (96514,10160,NULL,'requirement',96109,'sla_remind','in_app','您跟进的需求「LoRa 网关部署方案咨询」已超时未响应，请尽快处理','系统通知',0,0,NULL,'2026-07-18 07:30:00'),
 (96515,10160,NULL,'requirement',96105,'sla_remind','feishu','您关注的需求「工业网关 RedCap 方案」首响时限告警','系统通知',1,0,NULL,'2026-07-17 07:00:00');

-- =============================================================================
-- 10. subscription 订阅（10160 订阅 6 个 data.sql 分类；category_name 快照；uk(user,category) 唯一）
-- =============================================================================
INSERT INTO `subscription` (`id`,`user_id`,`category_id`,`category_name`,`source`,`create_time`) VALUES
 (96601,10160,1,'IoT 模组','default_dept','2026-07-01 09:00:00'),
 (96602,10160,101,'5G 模组','manual','2026-07-02 10:00:00'),
 (96603,10160,102,'NB-IoT 模组','manual','2026-07-02 10:05:00'),
 (96604,10160,103,'Cat.1 模组','manual','2026-07-03 11:00:00'),
 (96605,10160,2,'车载方案','manual','2026-07-05 14:00:00'),
 (96606,10160,301,'智慧城市','manual','2026-07-06 16:00:00');

-- =============================================================================
-- 11. view_log 浏览记录（62 行；user 轮流 9001-9004/10160；target 指向种下商机/需求；
--     viewed_at 分布近 24h 与近 7 天，支撑 home.active_users 与 dashboard uv/pv/hourly_active）
--     ⚠️ uk_view_user_target(user,target_type,target_id) 唯一：同一 user+target 仅一行。
-- =============================================================================
INSERT INTO `view_log` (`id`,`user_id`,`target_type`,`target_id`,`viewed_at`) VALUES
 -- 9001
 (96701,9001,'Opportunity',96001,DATE_SUB(NOW(),INTERVAL 2 HOUR)),
 (96702,9001,'Opportunity',96002,DATE_SUB(NOW(),INTERVAL 5 HOUR)),
 (96703,9001,'Opportunity',96003,DATE_SUB(NOW(),INTERVAL 8 HOUR)),
 (96704,9001,'Opportunity',96004,DATE_SUB(NOW(),INTERVAL 11 HOUR)),
 (96705,9001,'Opportunity',96005,DATE_SUB(NOW(),INTERVAL 14 HOUR)),
 (96706,9001,'Opportunity',96006,DATE_SUB(NOW(),INTERVAL 20 HOUR)),
 (96707,9001,'Request',96101,DATE_SUB(NOW(),INTERVAL 2 DAY)),
 (96708,9001,'Request',96102,DATE_SUB(NOW(),INTERVAL 3 DAY)),
 (96709,9001,'Request',96105,DATE_SUB(NOW(),INTERVAL 4 DAY)),
 (96710,9001,'Request',96107,DATE_SUB(NOW(),INTERVAL 5 DAY)),
 (96711,9001,'Request',96109,DATE_SUB(NOW(),INTERVAL 6 DAY)),
 (96712,9001,'Request',96111,DATE_SUB(NOW(),INTERVAL 7 DAY)),
 -- 9002
 (96713,9002,'Opportunity',96001,DATE_SUB(NOW(),INTERVAL 1 HOUR)),
 (96714,9002,'Opportunity',96003,DATE_SUB(NOW(),INTERVAL 3 HOUR)),
 (96715,9002,'Opportunity',96005,DATE_SUB(NOW(),INTERVAL 6 HOUR)),
 (96716,9002,'Opportunity',96007,DATE_SUB(NOW(),INTERVAL 9 HOUR)),
 (96717,9002,'Opportunity',96008,DATE_SUB(NOW(),INTERVAL 13 HOUR)),
 (96718,9002,'Opportunity',96010,DATE_SUB(NOW(),INTERVAL 18 HOUR)),
 (96719,9002,'Request',96101,DATE_SUB(NOW(),INTERVAL 2 DAY)),
 (96720,9002,'Request',96103,DATE_SUB(NOW(),INTERVAL 3 DAY)),
 (96721,9002,'Request',96105,DATE_SUB(NOW(),INTERVAL 4 DAY)),
 (96722,9002,'Request',96106,DATE_SUB(NOW(),INTERVAL 5 DAY)),
 (96723,9002,'Request',96108,DATE_SUB(NOW(),INTERVAL 6 DAY)),
 (96724,9002,'Request',96112,DATE_SUB(NOW(),INTERVAL 7 DAY)),
 -- 9003
 (96725,9003,'Opportunity',96002,DATE_SUB(NOW(),INTERVAL 4 HOUR)),
 (96726,9003,'Opportunity',96004,DATE_SUB(NOW(),INTERVAL 7 HOUR)),
 (96727,9003,'Opportunity',96006,DATE_SUB(NOW(),INTERVAL 10 HOUR)),
 (96728,9003,'Opportunity',96008,DATE_SUB(NOW(),INTERVAL 15 HOUR)),
 (96729,9003,'Opportunity',96009,DATE_SUB(NOW(),INTERVAL 19 HOUR)),
 (96730,9003,'Opportunity',96011,DATE_SUB(NOW(),INTERVAL 22 HOUR)),
 (96731,9003,'Request',96102,DATE_SUB(NOW(),INTERVAL 1 DAY)),
 (96732,9003,'Request',96104,DATE_SUB(NOW(),INTERVAL 2 DAY)),
 (96733,9003,'Request',96107,DATE_SUB(NOW(),INTERVAL 3 DAY)),
 (96734,9003,'Request',96109,DATE_SUB(NOW(),INTERVAL 4 DAY)),
 (96735,9003,'Request',96110,DATE_SUB(NOW(),INTERVAL 5 DAY)),
 (96736,9003,'Request',96111,DATE_SUB(NOW(),INTERVAL 6 DAY)),
 -- 9004
 (96737,9004,'Opportunity',96001,DATE_SUB(NOW(),INTERVAL 2 HOUR)),
 (96738,9004,'Opportunity',96002,DATE_SUB(NOW(),INTERVAL 5 HOUR)),
 (96739,9004,'Opportunity',96003,DATE_SUB(NOW(),INTERVAL 12 HOUR)),
 (96740,9004,'Opportunity',96007,DATE_SUB(NOW(),INTERVAL 16 HOUR)),
 (96741,9004,'Opportunity',96010,DATE_SUB(NOW(),INTERVAL 21 HOUR)),
 (96742,9004,'Opportunity',96011,DATE_SUB(NOW(),INTERVAL 23 HOUR)),
 (96743,9004,'Request',96101,DATE_SUB(NOW(),INTERVAL 2 DAY)),
 (96744,9004,'Request',96105,DATE_SUB(NOW(),INTERVAL 3 DAY)),
 (96745,9004,'Request',96106,DATE_SUB(NOW(),INTERVAL 4 DAY)),
 (96746,9004,'Request',96107,DATE_SUB(NOW(),INTERVAL 5 DAY)),
 (96747,9004,'Request',96108,DATE_SUB(NOW(),INTERVAL 6 DAY)),
 (96748,9004,'Request',96112,DATE_SUB(NOW(),INTERVAL 7 DAY)),
 -- 10160
 (96749,10160,'Opportunity',96001,DATE_SUB(NOW(),INTERVAL 1 HOUR)),
 (96750,10160,'Opportunity',96002,DATE_SUB(NOW(),INTERVAL 2 HOUR)),
 (96751,10160,'Opportunity',96003,DATE_SUB(NOW(),INTERVAL 4 HOUR)),
 (96752,10160,'Opportunity',96004,DATE_SUB(NOW(),INTERVAL 6 HOUR)),
 (96753,10160,'Opportunity',96005,DATE_SUB(NOW(),INTERVAL 8 HOUR)),
 (96754,10160,'Opportunity',96006,DATE_SUB(NOW(),INTERVAL 10 HOUR)),
 (96755,10160,'Opportunity',96007,DATE_SUB(NOW(),INTERVAL 12 HOUR)),
 (96756,10160,'Opportunity',96008,DATE_SUB(NOW(),INTERVAL 16 HOUR)),
 (96757,10160,'Request',96101,DATE_SUB(NOW(),INTERVAL 2 DAY)),
 (96758,10160,'Request',96102,DATE_SUB(NOW(),INTERVAL 3 DAY)),
 (96759,10160,'Request',96103,DATE_SUB(NOW(),INTERVAL 4 DAY)),
 (96760,10160,'Request',96105,DATE_SUB(NOW(),INTERVAL 5 DAY)),
 (96761,10160,'Request',96107,DATE_SUB(NOW(),INTERVAL 6 DAY)),
 (96762,10160,'Request',96109,DATE_SUB(NOW(),INTERVAL 7 DAY));

-- =============================================================================
-- 12. audit_log 操作日志（13 条；action_type 覆盖 publish/archive/delete/role_change/
--     isolation_change/category_change/sla_escalation/login；operator 轮流；ip 用 10.12.x.x；
--     before/after_snapshot 给合法 JSON 或 NULL）
-- =============================================================================
INSERT INTO `audit_log`
 (`id`,`operator_id`,`operator_name`,`action_type`,`target`,`result`,`ip_address`,`user_agent`,
  `before_snapshot`,`after_snapshot`,`create_time`) VALUES
 (96901,9002,'张伟','publish','方案：5G RedCap 模组 RG200U 产品选型方案','success','10.12.33.101',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0',NULL,'{"status":"published","publishedAt":"2026-07-19 10:00:00"}','2026-07-19 10:00:12'),
 (96902,9003,'李娜','archive','方案：Wi-Fi 6 + BLE 二合一模组 FCU630','success','10.12.33.102',
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) Safari/17.5','{"status":"published"}','{"status":"archived","reason":"内容过期"}','2026-07-19 16:42:08'),
 (96903,9004,'王强','delete','需求：某已撤回的定制需求','success','10.12.33.103',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Edge/126.0','{"status":"closed","id":"REQ0099"}',NULL,'2026-07-19 11:20:55'),
 (96904,9001,'运营管理员','role_change','用户：陈涛（chentao）','success','10.12.33.104',
  'Mozilla/5.0 (X11; Linux x86_64) Firefox/127.0','{"roles":["user"]}','{"roles":["user","operator"]}','2026-07-18 14:05:31'),
 (96905,NULL,'系统','sla_escalation','需求：急需高精度定位 RTK 模组','success','10.12.0.1',
  'sales-lead-hub-scheduler/1.0','{"slaLevel":"L1"}','{"slaLevel":"L2","notified":["产品线负责人"]}','2026-07-18 09:00:00'),
 (96906,10160,'atom.ye','login','运营后台','fail','10.12.44.77',
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5) Safari/17.5',NULL,'{"reason":"SSO 令牌校验失败"}','2026-07-18 08:31:19'),
 (96907,9003,'李娜','category_change','分类：NB-IoT 模组 → 停用','success','10.12.33.108',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0','{"isActive":true}','{"isActive":false}','2026-07-17 17:22:40'),
 (96908,9004,'王强','isolation_change','产品线：车载产品部 隔离策略','success','10.12.33.109',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0','{"isolation":"off"}','{"isolation":"on","scope":"dept"}','2026-07-17 10:11:03'),
 (96909,9001,'运营管理员','publish','公告：平台维护通知','fail','10.12.33.110',
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) Chrome/126.0','{"status":"draft"}','{"error":"推送渠道超时"}','2026-07-16 15:48:27'),
 (96910,9002,'张伟','delete','分类：已弃用旧分类','fail','10.12.33.111',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Edge/125.0','{"contentCount":12}','{"error":"存在关联内容，禁止删除"}','2026-07-16 09:15:50'),
 (96911,9003,'李娜','archive','方案：智能POS Cat.1 模组 EG915U','success','10.12.33.112',
  'Mozilla/5.0 (X11; Linux x86_64) Chrome/126.0','{"status":"published"}','{"status":"archived","reason":"涉嫌违规"}','2026-07-15 13:37:11'),
 (96912,9001,'运营管理员','role_change','用户：王强（wangqiang）','fail','10.12.33.113',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0','{"roles":["user","operator"]}','{"error":"权限不足，需超级管理员审批"}','2026-07-15 10:02:44'),
 (96913,9002,'张伟','login','运营后台','success','10.12.33.101',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0',NULL,'{"session":"renewed"}','2026-07-15 08:30:00');

-- =============================================================================
-- 13. product_line_member 补 owner（data.sql 已有 501/502/503 的 owner=601/603/604；
--     504 天线产品线 / 505 定位产品线 缺 owner，此处补 96951/96952 作 SLA L1 升级人）
--     uk_plm_line_user(product_line,user) 唯一：与 data.sql 既有行无冲突。
-- =============================================================================
INSERT INTO `product_line_member` (`id`,`product_line_id`,`user_id`,`user_name`,`is_owner`,`create_time`) VALUES
 (96951,504,9004,'王强',1,'2026-07-01 00:00:00'),
 (96952,505,9003,'李娜',1,'2026-07-01 00:00:00');

-- =============================================================================
-- 一致性自检清单（本次种子）
-- -----------------------------------------------------------------------------
-- [评论计数] comment_count = 该 target 的 interaction(type=comment) 行数：
--     96001=6(96301-96306) · 96002=3(96314-96316) · 96003=3(96322-96324) ·
--     96101=4(96329-96332) · 96105=3(96338-96340)。其余商机/需求未种评论行，comment_count 为
--     mock 展示值(商机)或 0(需求)，无 interaction 行与之矛盾。
-- [点赞计数] like_count = 该 target 的 interaction(type=like) 行数（仅热门 target）：
--     96001=4(96307-96310) · 96002=3(96317-96319) · 96003=2(96325-96326) ·
--     96101=3(96333-96335) · 96105=2(96341-96342)。
-- [收藏计数] collect_count = 该 target 的 interaction(type=collect) 行数（仅热门 target）：
--     96001=3(96311-96313) · 96002=2(96320-96321) · 96003=2(96327-96328) ·
--     96101=2(96336-96337) · 96105=2(96343-96344)。
-- [响应计数] response_count = 该需求 solution_response 行数：
--     96101=3(96201-96203) · 96102=2(96206-96207) · 96105=1(96208) · 96107=2(96204-96205) ·
--     96108=1(96209) · 96112=1(96210)；96103/96104/96106/96109/96110/96111 = 0（未种方案）。
-- [采纳双向] request.adopted_response_id ↔ solution_response.is_adopted=1：
--     96101↔96201 · 96107↔96204（各仅一条 is_adopted=1）。
-- [reaction_uk 唯一] like/collect 行的 (user_id,target_type,target_id,type) 组合互不重复（已逐行核对）。
-- [view_log 唯一] 每 (user_id,target_type,target_id) 仅一行（同一 user 的 target 列表无重复）。
-- [订阅唯一] subscription (user_id,category_id) 互不重复（10160 订阅 1/101/102/103/2/301 六个分类）。
-- [可见性] visibility_scope：dept 仅 96102(values=[1001])；personnel 仅 96104(values=[10160])；其余 all。
-- [强制确认] notification 仅 96505 is_force_confirm=1 且 confirm_time=NULL（未确认）。
-- [SLA 时间] 96106=NOW()-1h(剩余)；96109=NOW()-3d、96111=NOW()-5d、96103/2026-07-15(已超时)。
-- -----------------------------------------------------------------------------
-- 各表本次种子行数汇总：
--   opportunity           12   (96001-96012)
--   opportunity_category  16
--   opportunity_request   12   (96101-96112)
--   request_category      12
--   request_product_line  12
--   solution_response     10   (96201-96210)
--   interaction           44   (96301-96344；comment 19 / like 14 / collect 11)
--   announcement           8   (96401-96408)
--   notification          15   (96501-96515)
--   subscription           6   (96601-96606)
--   view_log              62   (96701-96762)
--   audit_log             13   (96901-96913)
--   product_line_member    2   (96951-96952)
--   合计                 224 行
-- =============================================================================
