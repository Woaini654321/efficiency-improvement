package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.constant.FeedbackAnonymizer;
import com.quectel.web.cloud.salesleadhubserver.convert.FeedbackConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.FeedbackDao;
import com.quectel.web.cloud.salesleadhubserver.dto.FeedbackCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.FeedbackErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.FeedbackDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.FeedbackService;
import com.quectel.web.cloud.salesleadhubserver.vo.FeedbackVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 吐槽墙 service。
 *
 * <p>全模块无 admin 门槛：登录（本地已开通）即可读写，运营治理下期。写操作调
 * {@code requireAnyRole(全部三角色)} 仅为 fail-closed 确保发帖人已在本平台开通
 * （create_by 落真实作者供反滥用），不做角色区分。</p>
 */
@Service
public class FeedbackServiceImpl implements FeedbackService {

    /** 列表上限：吐槽墙一次性拉全量（前端本地过滤），封顶防失控。 */
    private static final int LIST_LIMIT = 500;

    private final FeedbackDao dao;
    private final CurrentUserResolver currentUser;
    private final FeedbackConvert convert;

    public FeedbackServiceImpl(FeedbackDao dao,
                               CurrentUserResolver currentUser,
                               FeedbackConvert convert) {
        this.dao = dao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<FeedbackVO> list() {
        Page<FeedbackDO> p = new Page<>(1, LIST_LIMIT);
        IPage<FeedbackDO> r = dao.lambdaQuery()
                .orderByDesc(FeedbackDO::getCreateTime)
                .page(p);
        List<FeedbackVO> records = r.getRecords().stream()
                .map(convert::toVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public Long create(FeedbackCreateDTO dto) {
        // 登录即可（fail-closed 确保发帖人已在本平台开通，create_by 由框架落真实作者）
        currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);

        FeedbackDO d = convert.toCreateDO(dto);
        // 匿名昵称与卡片视觉由后端生成，不接受前端传入（防伪造匿名身份 / 绕过默认色板）
        d.setAnonName(FeedbackAnonymizer.nextAnonName());
        d.setEmoji(FeedbackAnonymizer.nextEmoji());
        d.setColor(FeedbackAnonymizer.nextColor());
        d.setLikeCount(0);

        dao.save(d);
        return d.getId();
    }

    @Override
    @Transactional
    public void like(Long id) {
        currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        // 原子自增，无去重（吐槽墙点赞不记名，简单自增即可）
        boolean ok = dao.increaseLikeCount(id);
        if (!ok) {
            throw new BaseException(FeedbackErrorCode.FEEDBACK_NOT_FOUND, "吐槽不存在");
        }
    }
}
