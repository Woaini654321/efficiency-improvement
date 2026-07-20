package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardCategoryDistVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardHotContentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardPageHeatVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardPieSegVO;
import org.springframework.stereotype.Component;

/**
 * 运营看板手写映射：把聚合出的原子数据装进各嵌套 VO。
 * 便于 VO 契约（snake_case）离线断言；复杂聚合（趋势/环比/分桶）留在 service。
 */
@Component
public class DashboardConvert {

    public DashboardHotContentVO toHotContentVO(OpportunityDO d) {
        DashboardHotContentVO v = new DashboardHotContentVO();
        v.setContentId(d.getId());
        v.setTitle(d.getTitle());
        v.setType(d.getType());
        v.setViewCount(d.getViewCount() == null ? 0 : d.getViewCount());
        return v;
    }

    public DashboardCategoryDistVO toCategoryDistVO(String name, int count, double percent) {
        DashboardCategoryDistVO v = new DashboardCategoryDistVO();
        v.setName(name);
        v.setCount(count);
        v.setPercent(percent);
        return v;
    }

    public DashboardPageHeatVO toPageHeatVO(String page, int count, double percent) {
        DashboardPageHeatVO v = new DashboardPageHeatVO();
        v.setPage(page);
        v.setCount(count);
        v.setPercent(percent);
        return v;
    }

    public DashboardPieSegVO toPieSegVO(String name, int value, String color) {
        DashboardPieSegVO v = new DashboardPieSegVO();
        v.setName(name);
        v.setValue(value);
        v.setColor(color);
        return v;
    }
}
