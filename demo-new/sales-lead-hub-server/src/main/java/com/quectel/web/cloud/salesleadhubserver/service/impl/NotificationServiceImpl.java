package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.NotificationConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPreferenceDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.NotificationErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.NotificationService;
import com.quectel.web.cloud.salesleadhubserver.vo.NotificationPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final String READ_YES = "read";
    private static final String READ_NO = "unread";
    private static final int FLAG_ON = 1;

    private final NotificationDao dao;
    private final SysUserDao sysUserDao;
    private final NotificationConvert convert;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationDao dao,
                                   SysUserDao sysUserDao,
                                   NotificationConvert convert,
                                   ObjectMapper objectMapper) {
        this.dao = dao;
        this.sysUserDao = sysUserDao;
        this.convert = convert;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<NotificationPageVO> page(NotificationPageDTO dto) {
        Long uid = requireCurrentUserId();
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);

        Page<NotificationDO> p = new Page<>(pageNumber, pageSize);
        IPage<NotificationDO> r = dao.lambdaQuery()
                // 归属硬边界：只查当前登录人自己的通知，绝不接受 userId 入参
                .eq(NotificationDO::getUserId, uid)
                .like(dto.getKeyword() != null && !dto.getKeyword().isEmpty(),
                        NotificationDO::getTitle, dto.getKeyword())
                .eq(dto.getType() != null && !dto.getType().isEmpty(),
                        NotificationDO::getType, dto.getType())
                .eq(READ_YES.equals(dto.getIsRead()), NotificationDO::getIsRead, FLAG_ON)
                .eq(READ_NO.equals(dto.getIsRead()), NotificationDO::getIsRead, 0)
                .orderByDesc(NotificationDO::getCreateTime)
                .page(p);

        List<NotificationPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        Long uid = requireCurrentUserId();
        NotificationDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        // 归属校验：不是自己的通知不许标记已读，否则可标记别人的
        if (d.getUserId() == null || !d.getUserId().equals(uid)) {
            throw new BaseException(NotificationErrorCode.NOT_OWNER, "无权操作他人的通知");
        }
        if (d.getIsRead() != null && d.getIsRead() == FLAG_ON) {
            return; // 幂等：已读再标记无副作用
        }
        d.setIsRead(FLAG_ON);
        // 强制确认类通知：读即视为确认，记确认时间（支撑 §7 48h 已读/确认率统计）
        if (d.getIsForceConfirm() != null && d.getIsForceConfirm() == FLAG_ON) {
            d.setConfirmTime(java.time.LocalDateTime.now());
        }
        dao.updateById(d);
    }

    @Override
    @Transactional
    public void markAllRead() {
        Long uid = requireCurrentUserId();
        // 只批量置己方 is_read=1；用 UpdateWrapper 单条 UPDATE，避免逐行读写
        LambdaUpdateWrapper<NotificationDO> w = new LambdaUpdateWrapper<NotificationDO>()
                .eq(NotificationDO::getUserId, uid)
                .eq(NotificationDO::getIsRead, 0)
                .set(NotificationDO::getIsRead, FLAG_ON);
        dao.update(w);
    }

    @Override
    @Transactional
    public void savePreference(NotificationPreferenceDTO dto) {
        // 偏好写在本人档案上：无本地档案即未开通，拒绝（fail-closed）
        Long uid = requireCurrentUserId();
        SysUserDO me = sysUserDao.getById(uid);
        if (me == null) {
            throw new BaseException(ErrorCode.FORBIDDEN, "当前账号尚未在本平台开通，无法保存偏好");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(dto.getMatrix());
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "通知偏好矩阵格式非法");
        }
        // 只更新 notification_preferences 一列，不碰乐观锁 version 等其它字段
        sysUserDao.update(new LambdaUpdateWrapper<SysUserDO>()
                .eq(SysUserDO::getId, uid)
                .set(SysUserDO::getNotificationPreferences, json));
    }

    // ---------- private ----------

    /** 当前登录人 id，取不到即未登录/上下文缺失，fail-closed 拒绝。 */
    private Long requireCurrentUserId() {
        Long uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            throw new BaseException(ErrorCode.FORBIDDEN, "未获取到登录用户，请重新登录");
        }
        return uid;
    }
}
