package com.github.kokoachino.model.enums;

import lombok.Getter;


/**
 * 事件类型枚举
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Getter
public enum EventType {

    // 水印模板相关
    TEMPLATE_CREATE("TEMPLATE_CREATE", "模板创建", "水印模板"),
    TEMPLATE_UPDATE("TEMPLATE_UPDATE", "模板修改", "水印模板"),
    TEMPLATE_DELETE("TEMPLATE_DELETE", "模板删除", "水印模板"),

    // 批量任务相关
    BATCH_TASK_SUBMIT("BATCH_TASK_SUBMIT", "任务提交", "批量任务"),
    BATCH_TASK_COMPLETE("BATCH_TASK_COMPLETE", "任务完成", "批量任务"),

    // 点数相关
    POINT_DEDUCT("POINT_DEDUCT", "点数预扣", "点数"),
    POINT_REFUND("POINT_REFUND", "点数返还", "点数"),
    POINT_RECHARGE("POINT_RECHARGE", "点数充值", "点数"),

    // 团队相关
    TEAM_JOIN("TEAM_JOIN", "加入团队", "团队"),
    TEAM_LEAVE("TEAM_LEAVE", "离开团队", "团队"),
    TEAM_TRANSFER("TEAM_TRANSFER", "队长转移", "团队"),

    // 字体相关
    FONT_UPLOAD("FONT_UPLOAD", "字体上传", "字体"),
    FONT_DELETE("FONT_DELETE", "字体删除", "字体");

    private final String code;
    private final String description;
    private final String category;

    EventType(String code, String description, String category) {
        this.code = code;
        this.description = description;
        this.category = category;
    }
}
