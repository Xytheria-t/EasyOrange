package com.cartethyia.easyorange.user.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class NicknameGenerator {

    private static final String[] ADJECTIVES = {
        "阳光", "活力", "快乐", "聪明", "可爱", "帅气", "机智", "乐观",
        "温柔", "善良", "勇敢", "真诚", "热情", "冷静", "大方", "细心",
        "帅气", "呆萌", "酷炫", "清新", "甜美", "文艺", "高冷", "傲娇",
        "暖心", "贴心", "淡定", "从容", "潇洒", "干练", "独立", "自信"
    };

    private static final String[] NOUNS = {
        "番茄", "橙子", "苹果", "香蕉", "葡萄", "柠檬", "芒果", "西瓜",
        "草莓", "蓝莓", "樱桃", "桃子", "柚子", "荔枝", "龙眼", "猕猴桃",
        "柠檬", "椰子", "榴莲", "菠萝", "火龙果", "石榴", "柿子", "枇杷",
        "土豆", "番茄", "玉米", "红薯", "黄瓜", "萝卜", "青菜", "白菜"
    };

    private static final String[] SUFFIXES = {
        "少年", "少女", "小子", "菇凉", "同学", "伙伴", "达人", "控",
        "酱", "桑", "君", "酱", "宝", "崽", "崽崽", "呀", "呀呀"
    };

    private static final String[] PATTERNS = {
        "%s%s",
        "%s%s%s",
        "%s%s%d",
        "%s%s%s%d",
        "%s%s_%d",
        "%s%d",
        "%s%s%d%d%d%d",
    };

    public String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = randomItem(random, ADJECTIVES);
        String noun = randomItem(random, NOUNS);
        String suffix = randomItem(random, SUFFIXES);
        int number = random.nextInt(10000);

        String pattern = randomItem(random, PATTERNS);
        return switch (pattern) {
            case "%s%s" -> adjective + noun;
            case "%s%s%s" -> adjective + noun + suffix;
            case "%s%s%s%d" -> adjective + noun + suffix + number;
            case "%s%s_%d" -> adjective + noun + "_" + number;
            case "%s%d" -> adjective + number;
            case "%s%s%d%d%d%d" -> adjective + noun
                + random.nextInt(10) + random.nextInt(10)
                + random.nextInt(10) + random.nextInt(10);
            default -> adjective + noun + number;
        };
    }

    private String randomItem(ThreadLocalRandom random, String[] array) {
        return array[random.nextInt(array.length)];
    }
}