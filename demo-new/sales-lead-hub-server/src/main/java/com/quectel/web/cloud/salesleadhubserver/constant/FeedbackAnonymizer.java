package com.quectel.web.cloud.salesleadhubserver.constant;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 吐槽墙匿名装饰器：生成匿名昵称、随机 emoji、随机卡片色。
 *
 * <p><b>匿名昵称</b>由「形容词 + 动物」词表随机组合（如「爱吐槽的水獭」），
 * 与真实身份无任何映射关系，纯随机，不可反推。<b>emoji/color</b> 前端 create 不传，
 * 从与 mock 一致的默认池随机取，保证卡片视觉与原型一致。</p>
 *
 * <p>全静态工具类，无状态；随机源用 {@link ThreadLocalRandom} 免竞争。</p>
 */
public final class FeedbackAnonymizer {

    /** 形容词词表 */
    private static final String[] ADJECTIVES = {
            "爱吐槽的", "匿名的", "摸鱼的", "熬夜的", "赶方案的", "被参数折磨的",
            "出差中的", "找方案的", "刚开完会的", "喝咖啡的", "改需求的", "跑客户的",
            "看报价的", "等审批的", "掉头发的", "很淡定的"
    };

    /** 动物词表 */
    private static final String[] ANIMALS = {
            "水獭", "柯基", "仓鼠", "猫头鹰", "刺猬", "羊驼", "海豹", "浣熊",
            "考拉", "企鹅", "河狸", "松鼠", "树懒", "水豚", "狐狸", "熊猫"
    };

    /** 默认 emoji 池（取自前端 feedback.json 实测值） */
    private static final String[] EMOJIS = {
            "😤", "🤯", "😩", "😮‍💨", "🫠", "📱", "🙄", "😅",
            "🥲", "👀", "🧭", "🎉", "🔄"
    };

    /** 默认卡片色池（取自前端 feedback.json 实测值） */
    private static final String[] COLORS = {
            "#eb2f96", "#fa8c16", "#722ed1", "#1890ff", "#13c2c2",
            "#faad14", "#2f54eb", "#52c41a"
    };

    private FeedbackAnonymizer() {
    }

    /** 随机匿名昵称：形容词 + 动物。 */
    public static String nextAnonName() {
        return pick(ADJECTIVES) + pick(ANIMALS);
    }

    /** 随机默认 emoji。 */
    public static String nextEmoji() {
        return pick(EMOJIS);
    }

    /** 随机默认卡片色。 */
    public static String nextColor() {
        return pick(COLORS);
    }

    private static String pick(String[] pool) {
        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }
}
