package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementAdoptDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCloseDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.ResponseCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.RequirementErrorCode;
import com.quectel.web.cloud.salesleadhubserver.exception.ResponseErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SolutionResponseDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.ResponseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 需求-方案匹配闭环实现。
 *
 * <p>鉴权口径：提交方案「登录即可」（任一已开通业务角色皆可，用 requireAnyRole 覆盖三角色，
 * 等价于「已开通用户」）；采纳/关闭仅需求发布人或 admin。业务角色查本地 sys_user，
 * 不用 UAA/@PreAuthorize（见 CurrentUserResolver）。</p>
 *
 * <p>状态门槛的单一真相源是需求主表：{@code status=='Adopted'} 或
 * {@code adopted_response_id!=null} 即视为已采纳；{@code status=='Closed'} 即已关闭。</p>
 */
@Service
public class ResponseServiceImpl implements ResponseService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_COLLECTING = "Collecting";
    private static final String STATUS_ADOPTED = "Adopted";
    private static final String STATUS_CLOSED = "Closed";

    private final RequirementDao requirementDao;
    private final SolutionResponseDao solutionResponseDao;
    private final NotificationDao notificationDao;
    private final CurrentUserResolver currentUser;

    public ResponseServiceImpl(RequirementDao requirementDao,
                               SolutionResponseDao solutionResponseDao,
                               NotificationDao notificationDao,
                               CurrentUserResolver currentUser) {
        this.requirementDao = requirementDao;
        this.solutionResponseDao = solutionResponseDao;
        this.notificationDao = notificationDao;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public Long create(ResponseCreateDTO dto) {
        // 登录即可：三业务角色任一放行 = 已开通用户
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);

        OpportunityRequestDO req = requirementDao.getById(dto.getRequestId());
        if (req == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        requireOpenForResponse(req);

        SolutionResponseDO r = new SolutionResponseDO();
        r.setRequestId(req.getId());
        r.setContent(dto.getContent());
        r.setAttachments(dto.getAttachments());
        r.setEmailRecipients(dto.getEmailRecipients());
        r.setCustomEmailRecipients(dto.getCustomEmailRecipients());
        r.setFeishuSync(Boolean.TRUE.equals(dto.getFeishuSync()) ? 1 : 0);
        // 响应人快照取自本地 sys_user 同一行（防客户端伪造，理由同发布人回填）
        r.setResponderId(me.getId());
        r.setResponderName(me.getName());
        r.setDepartmentId(me.getDepartmentId());
        r.setResponderDeptName(me.getDepartmentName());
        r.setIsAdopted(0);
        solutionResponseDao.save(r);

        // 同事务：response_count 原子 +1 且 Pending→Collecting（首响停表由 count>0 派生）
        requirementDao.increaseResponseCount(req.getId());

        // 同事务：通知需求发布人（快照 title/触发人名）
        if (req.getPublisherId() != null) {
            NotificationDO n = baseNotification(req.getPublisherId(), "response", req.getId(),
                    "收到新方案：" + req.getTitle(), me.getName());
            notificationDao.save(n);
        }
        return r.getId();
    }

    @Override
    @Transactional
    public void adopt(RequirementAdoptDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);

        OpportunityRequestDO req = requirementDao.getById(dto.getId());
        if (req == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        requireOwnerOrAdmin(req, me, "只能采纳自己发布的需求的方案");
        requireOpenForResponse(req);   // 已采纳/已关闭都拒（采纳单一真相源）

        SolutionResponseDO resp = solutionResponseDao.getById(dto.getResponseId());
        if (resp == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "方案不存在");
        }
        if (!req.getId().equals(resp.getRequestId())) {
            throw new BaseException(ResponseErrorCode.RESPONSE_NOT_MATCH, "该方案不属于此需求");
        }

        // 双写同事务：需求主表记采纳（单一真相源，adoptedResponseId 走 ALWAYS 策略），方案置冗余标记
        req.setAdoptedResponseId(dto.getResponseId());
        req.setStatus(STATUS_ADOPTED);
        if (!requirementDao.updateById(req)) {   // 乐观锁：WHERE id=? AND version=?
            throw new BaseException(RequirementErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
        resp.setIsAdopted(1);
        solutionResponseDao.updateById(resp);

        // 通知响应人被采纳
        if (resp.getResponderId() != null) {
            NotificationDO n = baseNotification(resp.getResponderId(), "adopt", req.getId(),
                    "方案被采纳：" + req.getTitle(), me.getName());
            notificationDao.save(n);
        }
    }

    /**
     * 关闭需求。仅发布人或 admin；Pending/Collecting → Closed。
     *
     * <p><b>审计锚点</b>：本操作「谁在何时关闭了哪条需求」的 audit_log 落库，规划由
     * {@code AuditLogAspect} 审计切面统一补（下期给本方法加 {@code @AuditAction("archive")}，
     * Closed 是需求侧的下架等价态）。本期先留此 javadoc 锚点，不在此手写审计行。</p>
     */
    @Override
    @Transactional
    public void close(RequirementCloseDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);

        OpportunityRequestDO req = requirementDao.getById(dto.getId());
        if (req == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        requireOwnerOrAdmin(req, me, "只能关闭自己发布的需求");

        String s = req.getStatus();
        if (!STATUS_PENDING.equals(s) && !STATUS_COLLECTING.equals(s)) {
            throw new BaseException(ResponseErrorCode.ILLEGAL_CLOSE_STATE,
                    "仅待响应/收集中的需求可关闭：当前 " + s);
        }
        req.setStatus(STATUS_CLOSED);
        if (!requirementDao.updateById(req)) {
            throw new BaseException(RequirementErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
    }

    // ---------- private ----------

    /** 校验需求仍开放（未采纳、未关闭），否则抛对应冲突码。 */
    private void requireOpenForResponse(OpportunityRequestDO req) {
        if (STATUS_CLOSED.equals(req.getStatus())) {
            throw new BaseException(ResponseErrorCode.REQUIREMENT_CLOSED, "需求已关闭");
        }
        if (STATUS_ADOPTED.equals(req.getStatus()) || req.getAdoptedResponseId() != null) {
            throw new BaseException(ResponseErrorCode.REQUIREMENT_ALREADY_ADOPTED, "需求已采纳方案");
        }
    }

    private void requireOwnerOrAdmin(OpportunityRequestDO req, SysUserDO me, String message) {
        boolean owner = req.getPublisherId() != null && req.getPublisherId().equals(me.getId());
        if (!owner && !CurrentUserResolver.ROLE_ADMIN.equals(me.getRole())) {
            throw new BaseException(RequirementErrorCode.NOT_OWNER, message);
        }
    }

    /** 构造一条站内通知（in_app），公共快照字段一次填齐。 */
    private NotificationDO baseNotification(Long userId, String type, Long requestId,
                                            String title, String triggerUserName) {
        NotificationDO n = new NotificationDO();
        n.setUserId(userId);
        n.setType(type);
        n.setChannel("in_app");
        n.setTargetType("Request");
        n.setTargetId(requestId);
        n.setTitle(title);
        n.setTriggerUserName(triggerUserName);
        n.setIsRead(0);
        n.setIsForceConfirm(0);
        return n;
    }
}
