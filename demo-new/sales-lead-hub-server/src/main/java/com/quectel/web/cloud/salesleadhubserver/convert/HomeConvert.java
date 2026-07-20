package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeAnnouncementVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomePostVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeSolutionVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeStatsVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeTaskVO;
import org.springframework.stereotype.Component;

/**
 * 首页工作台手写映射：各实体 DO → 对应嵌套 VO。
 *
 * <p>派生标志（是否逾期、是否订阅、名次）由 service 算好后传入，convert 只做扁平赋值，
 * 便于 VO 契约（snake_case + 日期格式）离线断言。</p>
 */
@Component
public class HomeConvert {

    public HomeStatsVO toStats(int solutionTotal, int pendingRequests, int weekDiscussions, int activeUsers) {
        HomeStatsVO v = new HomeStatsVO();
        v.setSolutionTotal(solutionTotal);
        v.setPendingRequests(pendingRequests);
        v.setWeekDiscussions(weekDiscussions);
        v.setActiveUsers(activeUsers);
        return v;
    }

    public HomeTaskVO toTaskVO(MeetingTaskDO d, boolean isOverdue) {
        HomeTaskVO v = new HomeTaskVO();
        v.setTaskId(d.getId());
        v.setTitle(d.getTaskDesc());
        v.setMeetingName(d.getMeetingName());
        v.setDeadline(d.getDeadline());
        v.setPriority(d.getPriority());
        v.setStatus(d.getStatus());
        v.setIsOverdue(isOverdue);
        return v;
    }

    public HomeSolutionVO toSolutionVO(OpportunityDO d, int rank, boolean isSubscribed) {
        HomeSolutionVO v = new HomeSolutionVO();
        v.setOpportunityId(d.getId());
        v.setRank(rank);
        v.setTitle(d.getTitle());
        v.setType(d.getType());
        v.setViewCount(nz(d.getViewCount()));
        v.setPublisherName(d.getPublisherName());
        v.setIsSubscribed(isSubscribed);
        return v;
    }

    public HomeAnnouncementVO toAnnouncementVO(AnnouncementDO d) {
        HomeAnnouncementVO v = new HomeAnnouncementVO();
        v.setAnnouncementId(d.getId());
        v.setTitle(d.getTitle());
        v.setType(d.getType());
        v.setPublisherName(d.getPublisherName());
        v.setPublishedAt(d.getPublishedAt());
        v.setViewCount(nz(d.getViewCount()));
        v.setIsPinned(d.getIsPinned() != null && d.getIsPinned() == 1);
        return v;
    }

    public HomePostVO toPostVO(DiscussionPostDO d) {
        HomePostVO v = new HomePostVO();
        v.setPostId(d.getId());
        v.setTopic(d.getTopic());
        v.setTitle(d.getTitle());
        v.setAuthorName(d.getAuthorName());
        v.setReplyCount(nz(d.getReplyCount()));
        v.setViewCount(nz(d.getViewCount()));
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    private int nz(Integer i) {
        return i == null ? 0 : i;
    }
}
