package com.quectel.web.cloud.salesleadhubserver.security;

import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.dao.AnnouncementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.AuditLogDao;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.CompetitorIntelDao;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionPostDao;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionReplyDao;
import com.quectel.web.cloud.salesleadhubserver.dao.FeedbackDao;
import com.quectel.web.cloud.salesleadhubserver.dao.IndustryIntelDao;
import com.quectel.web.cloud.salesleadhubserver.dao.InteractionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SubscriptionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ViewLogDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.ViewLogMapper;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.impl.AnnouncementServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.AuditLogServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.AuditServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.BatchServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.CategoryServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.DashboardServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.DiscussionServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.FeedbackServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.IntelServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.MeetingServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.MemberServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.NotificationServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.OpportunityServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.ProfileServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.RequirementServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.ResponseServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.SlaServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 鉴权负测矩阵：把全仓「admin/角色门」写方法与管理端点一次性钉死。
 *
 * <p>思路：每条用例是一个自包含的 {@link Executable}——就地 new 出目标 ServiceImpl（依赖全 mock），
 * 让鉴权入口失败（Group A：{@code CurrentUserResolver.requireAnyRole} mock 成抛
 * {@code BaseException(FORBIDDEN)}；Group B：无 CurrentUserResolver、直接用 SecurityUtils，
 * 静态 mock 成无登录返 null；Profile：{@code currentOrNull()} 返 null 触发 fail-closed），
 * 断言方法抛 {@link BaseException} 且所有 dao mock 零交互（鉴权先于任何查库/写库）。</p>
 *
 * <p>DTO/主键参数一律传 null/占位：因鉴权是每个方法的<b>第一条语句</b>，抛出时尚未触碰入参，
 * 故无需构造真实 payload。convert/calculator/objectMapper 等纯装配依赖同理传 null。</p>
 *
 * <p><b>未纳入矩阵的方法与原因</b>：
 * <ul>
 *   <li>{@code home.dashboard()}——实现明确「登录即可、取不到当前用户时个性化段安全降级为空、
 *       不阻断整页」，即<b>允许匿名</b>，无鉴权门可测，故不列（HomeServiceImpl 类注释为证）。</li>
 *   <li>各前台只读端点（{@code announce.frontPage/frontDetail}、{@code opportunity.page/detail}、
 *       {@code requirement.page/detail}、intel/discussion 的 page/detail、{@code feedback.list} 等）
 *       ——「登录即可、不校业务角色」，非本矩阵的 admin/角色门范畴。</li>
 *   <li>{@code meeting.page}、{@code task.page}——虽同样 requireAnyRole，但属读路径，
 *       其鉴权由各自 *ServiceImplTest 覆盖，本矩阵聚焦写/管理端点。</li>
 *   <li>{@code feedback.like}、{@code notification} 的读计数等幂等自增——非角色门。</li>
 *   <li>互动模块（点赞/收藏/评论）——无业务角色门槛（仅 currentOrNull 校已开通），
 *       归属边界由 InteractionServiceImplTest 覆盖。</li>
 * </ul>
 * 说明：{@code response.adopt/close} 在实现上属 {@code ResponseService}（需求-方案闭环），
 * 但按业务域归在「需求」下命名，构造的是 {@code ResponseServiceImpl}。</p>
 */
class RoleGateMatrixTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("gateCases")
    void gatedMethod_is_denied_and_touches_no_dao(String name, Executable probe) throws Throwable {
        probe.execute();
    }

    /** 造一个「一律拒绝」的 CurrentUserResolver：requireAnyRole 抛 FORBIDDEN。 */
    private static CurrentUserResolver deny() {
        CurrentUserResolver r = mock(CurrentUserResolver.class);
        when(r.requireAnyRole(any())).thenThrow(new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        return r;
    }

    private static Arguments c(String name, Executable e) {
        return Arguments.of(name, e);
    }

    private static Stream<Arguments> gateCases() {
        return Stream.of(

                // ---------- 运营内容审核 audit（全端点仅 admin）----------
                c("audit.page", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    RequirementDao req = mock(RequirementDao.class);
                    AuditServiceImpl s = new AuditServiceImpl(opp, req, deny(), null);
                    assertThrows(BaseException.class, () -> s.page(null));
                    verifyNoInteractions(opp, req);
                }),
                c("audit.changeStatus", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    RequirementDao req = mock(RequirementDao.class);
                    AuditServiceImpl s = new AuditServiceImpl(opp, req, deny(), null);
                    assertThrows(BaseException.class, () -> s.changeStatus(1L, "archived"));
                    verifyNoInteractions(opp, req);
                }),
                c("audit.changePin", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    RequirementDao req = mock(RequirementDao.class);
                    AuditServiceImpl s = new AuditServiceImpl(opp, req, deny(), null);
                    assertThrows(BaseException.class, () -> s.changePin(null));
                    verifyNoInteractions(opp, req);
                }),
                c("audit.delete", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    RequirementDao req = mock(RequirementDao.class);
                    AuditServiceImpl s = new AuditServiceImpl(opp, req, deny(), null);
                    assertThrows(BaseException.class, () -> s.delete(1L));
                    verifyNoInteractions(opp, req);
                }),

                // ---------- 审计日志 auditLog（仅 admin）----------
                c("auditLog.page", () -> {
                    AuditLogDao dao = mock(AuditLogDao.class);
                    AuditLogServiceImpl s = new AuditLogServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.page(null));
                    verifyNoInteractions(dao);
                }),

                // ---------- 公告运营端 7 端点（全部仅 admin）----------
                c("announce.operationPage", () -> {
                    AnnouncementDao dao = mock(AnnouncementDao.class);
                    AnnouncementServiceImpl s = new AnnouncementServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.operationPage(null));
                    verifyNoInteractions(dao);
                }),
                c("announce.operationDetail", () -> {
                    AnnouncementDao dao = mock(AnnouncementDao.class);
                    AnnouncementServiceImpl s = new AnnouncementServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.operationDetail(1L));
                    verifyNoInteractions(dao);
                }),
                c("announce.create", () -> {
                    AnnouncementDao dao = mock(AnnouncementDao.class);
                    AnnouncementServiceImpl s = new AnnouncementServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(dao);
                }),
                c("announce.update", () -> {
                    AnnouncementDao dao = mock(AnnouncementDao.class);
                    AnnouncementServiceImpl s = new AnnouncementServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.update(null));
                    verifyNoInteractions(dao);
                }),
                c("announce.changeStatus", () -> {
                    AnnouncementDao dao = mock(AnnouncementDao.class);
                    AnnouncementServiceImpl s = new AnnouncementServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.changeStatus(1L, "published"));
                    verifyNoInteractions(dao);
                }),
                c("announce.delete", () -> {
                    AnnouncementDao dao = mock(AnnouncementDao.class);
                    AnnouncementServiceImpl s = new AnnouncementServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.delete(1L));
                    verifyNoInteractions(dao);
                }),
                c("announce.stats", () -> {
                    AnnouncementDao dao = mock(AnnouncementDao.class);
                    AnnouncementServiceImpl s = new AnnouncementServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, s::stats);
                    verifyNoInteractions(dao);
                }),

                // ---------- 分类维护 category 写 4（仅 admin）----------
                c("category.create", () -> {
                    CategoryDao dao = mock(CategoryDao.class);
                    RequestCategoryDao rc = mock(RequestCategoryDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    CategoryServiceImpl s = new CategoryServiceImpl(dao, rc, oc, deny(), null);
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(dao, rc, oc);
                }),
                c("category.update", () -> {
                    CategoryDao dao = mock(CategoryDao.class);
                    RequestCategoryDao rc = mock(RequestCategoryDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    CategoryServiceImpl s = new CategoryServiceImpl(dao, rc, oc, deny(), null);
                    assertThrows(BaseException.class, () -> s.update(null));
                    verifyNoInteractions(dao, rc, oc);
                }),
                c("category.delete", () -> {
                    CategoryDao dao = mock(CategoryDao.class);
                    RequestCategoryDao rc = mock(RequestCategoryDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    CategoryServiceImpl s = new CategoryServiceImpl(dao, rc, oc, deny(), null);
                    assertThrows(BaseException.class, () -> s.delete(1L));
                    verifyNoInteractions(dao, rc, oc);
                }),
                c("category.changeActive", () -> {
                    CategoryDao dao = mock(CategoryDao.class);
                    RequestCategoryDao rc = mock(RequestCategoryDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    CategoryServiceImpl s = new CategoryServiceImpl(dao, rc, oc, deny(), null);
                    assertThrows(BaseException.class, () -> s.changeActive(1L, true));
                    verifyNoInteractions(dao, rc, oc);
                }),

                // ---------- 产品线成员 member 写 2（仅 admin）----------
                c("member.add", () -> {
                    ProductLineMemberDao m = mock(ProductLineMemberDao.class);
                    ProductLineDao pl = mock(ProductLineDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    MemberServiceImpl s = new MemberServiceImpl(m, pl, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.add(null));
                    verifyNoInteractions(m, pl, su);
                }),
                c("member.update", () -> {
                    ProductLineMemberDao m = mock(ProductLineMemberDao.class);
                    ProductLineDao pl = mock(ProductLineDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    MemberServiceImpl s = new MemberServiceImpl(m, pl, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.update(null));
                    verifyNoInteractions(m, pl, su);
                }),

                // ---------- 会议任务管理 meeting 4（仅 admin）----------
                c("meeting.create", () -> {
                    MeetingDao md = mock(MeetingDao.class);
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    MeetingServiceImpl s = new MeetingServiceImpl(md, td, deny(), null);
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(md, td);
                }),
                c("meeting.update", () -> {
                    MeetingDao md = mock(MeetingDao.class);
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    MeetingServiceImpl s = new MeetingServiceImpl(md, td, deny(), null);
                    assertThrows(BaseException.class, () -> s.update(null));
                    verifyNoInteractions(md, td);
                }),
                c("meeting.urge", () -> {
                    MeetingDao md = mock(MeetingDao.class);
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    MeetingServiceImpl s = new MeetingServiceImpl(md, td, deny(), null);
                    assertThrows(BaseException.class, () -> s.urge(1L, "x"));
                    verifyNoInteractions(md, td);
                }),
                c("meeting.cancel", () -> {
                    MeetingDao md = mock(MeetingDao.class);
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    MeetingServiceImpl s = new MeetingServiceImpl(md, td, deny(), null);
                    assertThrows(BaseException.class, () -> s.cancel(1L, "x"));
                    verifyNoInteractions(md, td);
                }),

                // ---------- 我的任务 task 3（非 assignee 亦被角色门先拦）----------
                c("task.start", () -> {
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    TaskServiceImpl s = new TaskServiceImpl(td, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.start(1L));
                    verifyNoInteractions(td, su);
                }),
                c("task.complete", () -> {
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    TaskServiceImpl s = new TaskServiceImpl(td, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.complete(1L, "x"));
                    verifyNoInteractions(td, su);
                }),
                c("task.transfer", () -> {
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    TaskServiceImpl s = new TaskServiceImpl(td, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.transfer(null));
                    verifyNoInteractions(td, su);
                }),

                // ---------- 批量发布 batch 2（仅 admin）----------
                c("batch.meta", () -> {
                    MeetingDao md = mock(MeetingDao.class);
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    BatchServiceImpl s = new BatchServiceImpl(md, td, su, deny(), null);
                    assertThrows(BaseException.class, s::meta);
                    verifyNoInteractions(md, td, su);
                }),
                c("batch.publish", () -> {
                    MeetingDao md = mock(MeetingDao.class);
                    MeetingTaskDao td = mock(MeetingTaskDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    BatchServiceImpl s = new BatchServiceImpl(md, td, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.publish(null));
                    verifyNoInteractions(md, td, su);
                }),

                // ---------- SLA 时效监控 4（全端点仅 admin）----------
                c("sla.page", () -> {
                    Object[] d = slaDaos();
                    SlaServiceImpl s = newSla(d);
                    assertThrows(BaseException.class, () -> s.page(null));
                    verifyNoInteractions(d);
                }),
                c("sla.stats", () -> {
                    Object[] d = slaDaos();
                    SlaServiceImpl s = newSla(d);
                    assertThrows(BaseException.class, s::stats);
                    verifyNoInteractions(d);
                }),
                c("sla.meta", () -> {
                    Object[] d = slaDaos();
                    SlaServiceImpl s = newSla(d);
                    assertThrows(BaseException.class, s::meta);
                    verifyNoInteractions(d);
                }),
                c("sla.urge", () -> {
                    Object[] d = slaDaos();
                    SlaServiceImpl s = newSla(d);
                    assertThrows(BaseException.class, () -> s.urge(null));
                    verifyNoInteractions(d);
                }),

                // ---------- 运营看板 dashboard 1（仅 admin）----------
                c("dashboard.dashboard", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    RequirementDao req = mock(RequirementDao.class);
                    ViewLogDao vl = mock(ViewLogDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    RequestCategoryDao rc = mock(RequestCategoryDao.class);
                    CategoryDao cat = mock(CategoryDao.class);
                    DashboardServiceImpl s = new DashboardServiceImpl(opp, req, vl, oc, rc, cat, deny(), null);
                    assertThrows(BaseException.class, () -> s.dashboard(null));
                    verifyNoInteractions(opp, req, vl, oc, rc, cat);
                }),

                // ---------- 商机 opportunity 写 3 ----------
                c("opportunity.create", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    CategoryDao cat = mock(CategoryDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    OpportunityServiceImpl s = new OpportunityServiceImpl(opp, cat, oc, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(opp, cat, oc, su);
                }),
                c("opportunity.update", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    CategoryDao cat = mock(CategoryDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    OpportunityServiceImpl s = new OpportunityServiceImpl(opp, cat, oc, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.update(null));
                    verifyNoInteractions(opp, cat, oc, su);
                }),
                c("opportunity.changeStatus", () -> {
                    OpportunityDao opp = mock(OpportunityDao.class);
                    CategoryDao cat = mock(CategoryDao.class);
                    OpportunityCategoryDao oc = mock(OpportunityCategoryDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    OpportunityServiceImpl s = new OpportunityServiceImpl(opp, cat, oc, su, deny(), null);
                    assertThrows(BaseException.class, () -> s.changeStatus(1L, "archived"));
                    verifyNoInteractions(opp, cat, oc, su);
                }),

                // ---------- 需求 requirement 写 2 + 闭环 adopt/close（ResponseServiceImpl）----------
                c("requirement.create", () -> {
                    RequirementDao req = mock(RequirementDao.class);
                    CategoryDao cat = mock(CategoryDao.class);
                    RequestCategoryDao rc = mock(RequestCategoryDao.class);
                    SolutionResponseDao sr = mock(SolutionResponseDao.class);
                    RequirementServiceImpl s = new RequirementServiceImpl(req, cat, rc, sr, deny(), null);
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(req, cat, rc, sr);
                }),
                c("requirement.update", () -> {
                    RequirementDao req = mock(RequirementDao.class);
                    CategoryDao cat = mock(CategoryDao.class);
                    RequestCategoryDao rc = mock(RequestCategoryDao.class);
                    SolutionResponseDao sr = mock(SolutionResponseDao.class);
                    RequirementServiceImpl s = new RequirementServiceImpl(req, cat, rc, sr, deny(), null);
                    assertThrows(BaseException.class, () -> s.update(null));
                    verifyNoInteractions(req, cat, rc, sr);
                }),
                c("requirement.adopt", () -> {
                    RequirementDao req = mock(RequirementDao.class);
                    SolutionResponseDao sr = mock(SolutionResponseDao.class);
                    NotificationDao nd = mock(NotificationDao.class);
                    ResponseServiceImpl s = new ResponseServiceImpl(req, sr, nd, deny());
                    assertThrows(BaseException.class, () -> s.adopt(null));
                    verifyNoInteractions(req, sr, nd);
                }),
                c("requirement.close", () -> {
                    RequirementDao req = mock(RequirementDao.class);
                    SolutionResponseDao sr = mock(SolutionResponseDao.class);
                    NotificationDao nd = mock(NotificationDao.class);
                    ResponseServiceImpl s = new ResponseServiceImpl(req, sr, nd, deny());
                    assertThrows(BaseException.class, () -> s.close(null));
                    verifyNoInteractions(req, sr, nd);
                }),

                // ---------- 方案提交 response.create（登录即可，未开通被 fail-closed 拦）----------
                c("response.create", () -> {
                    RequirementDao req = mock(RequirementDao.class);
                    SolutionResponseDao sr = mock(SolutionResponseDao.class);
                    NotificationDao nd = mock(NotificationDao.class);
                    ResponseServiceImpl s = new ResponseServiceImpl(req, sr, nd, deny());
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(req, sr, nd);
                }),

                // ---------- 讨论区 discussion 写 2 ----------
                c("discussion.create", () -> {
                    DiscussionPostDao pd = mock(DiscussionPostDao.class);
                    DiscussionReplyDao rd = mock(DiscussionReplyDao.class);
                    DiscussionServiceImpl s = new DiscussionServiceImpl(pd, rd, deny(), null);
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(pd, rd);
                }),
                c("discussion.reply", () -> {
                    DiscussionPostDao pd = mock(DiscussionPostDao.class);
                    DiscussionReplyDao rd = mock(DiscussionReplyDao.class);
                    DiscussionServiceImpl s = new DiscussionServiceImpl(pd, rd, deny(), null);
                    assertThrows(BaseException.class, () -> s.reply(null));
                    verifyNoInteractions(pd, rd);
                }),

                // ---------- 情报中心 intel.submit ----------
                c("intel.submitCompetitor", () -> {
                    CompetitorIntelDao cd = mock(CompetitorIntelDao.class);
                    IndustryIntelDao id = mock(IndustryIntelDao.class);
                    IntelServiceImpl s = new IntelServiceImpl(cd, id, deny(), null);
                    assertThrows(BaseException.class, () -> s.submitCompetitor(null));
                    verifyNoInteractions(cd, id);
                }),

                // ---------- 吐槽墙 feedback.create ----------
                c("feedback.create", () -> {
                    FeedbackDao dao = mock(FeedbackDao.class);
                    FeedbackServiceImpl s = new FeedbackServiceImpl(dao, deny(), null);
                    assertThrows(BaseException.class, () -> s.create(null));
                    verifyNoInteractions(dao);
                }),

                // ---------- 通知 notification 4（未登录，SecurityUtils 静态 mock 返 null）----------
                c("notification.page", () -> {
                    NotificationDao dao = mock(NotificationDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    try (org.mockito.MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
                        sec.when(SecurityUtils::getCurrentUserId).thenReturn(null);
                        NotificationServiceImpl s = new NotificationServiceImpl(dao, su, null, null);
                        assertThrows(BaseException.class, () -> s.page(null));
                    }
                    verifyNoInteractions(dao, su);
                }),
                c("notification.markRead", () -> {
                    NotificationDao dao = mock(NotificationDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    try (org.mockito.MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
                        sec.when(SecurityUtils::getCurrentUserId).thenReturn(null);
                        NotificationServiceImpl s = new NotificationServiceImpl(dao, su, null, null);
                        assertThrows(BaseException.class, () -> s.markRead(1L));
                    }
                    verifyNoInteractions(dao, su);
                }),
                c("notification.markAllRead", () -> {
                    NotificationDao dao = mock(NotificationDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    try (org.mockito.MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
                        sec.when(SecurityUtils::getCurrentUserId).thenReturn(null);
                        NotificationServiceImpl s = new NotificationServiceImpl(dao, su, null, null);
                        assertThrows(BaseException.class, s::markAllRead);
                    }
                    verifyNoInteractions(dao, su);
                }),
                c("notification.savePreference", () -> {
                    NotificationDao dao = mock(NotificationDao.class);
                    SysUserDao su = mock(SysUserDao.class);
                    try (org.mockito.MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
                        sec.when(SecurityUtils::getCurrentUserId).thenReturn(null);
                        NotificationServiceImpl s = new NotificationServiceImpl(dao, su, null, null);
                        assertThrows(BaseException.class, () -> s.savePreference(null));
                    }
                    verifyNoInteractions(dao, su);
                }),

                // ---------- 个人中心 profile.center（未开通/未登录，currentOrNull 返 null）----------
                c("profile.center", () -> {
                    CurrentUserResolver cur = mock(CurrentUserResolver.class);  // currentOrNull() 默认返 null
                    OpportunityDao opp = mock(OpportunityDao.class);
                    RequirementDao req = mock(RequirementDao.class);
                    SubscriptionDao sub = mock(SubscriptionDao.class);
                    CategoryDao cat = mock(CategoryDao.class);
                    InteractionDao inter = mock(InteractionDao.class);
                    SolutionResponseDao sr = mock(SolutionResponseDao.class);
                    ViewLogMapper vlm = mock(ViewLogMapper.class);
                    ProfileServiceImpl s = new ProfileServiceImpl(cur, opp, req, sub, cat, inter, sr, vlm, null);
                    assertThrows(BaseException.class, s::center);
                    verifyNoInteractions(opp, req, sub, cat, inter, sr, vlm);
                })
        );
    }

    // ---------- SLA 依赖较多，抽出装配助手保持用例两行内可读 ----------

    /** 造 SLA 的 7 个 dao mock（顺序与构造器一致）。 */
    private static Object[] slaDaos() {
        return new Object[]{
                mock(RequirementDao.class),
                mock(ProductLineMemberDao.class),
                mock(ProductLineDao.class),
                mock(RequestProductLineDao.class),
                mock(SysDepartmentDao.class),
                mock(SysUserDao.class),
                mock(NotificationDao.class)
        };
    }

    private static SlaServiceImpl newSla(Object[] d) {
        return new SlaServiceImpl(
                (RequirementDao) d[0], (ProductLineMemberDao) d[1], (ProductLineDao) d[2],
                (RequestProductLineDao) d[3], (SysDepartmentDao) d[4], (SysUserDao) d[5],
                (NotificationDao) d[6], deny(), null, null);
    }
}
