package com.github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 锁动作枚举
 * 用于区分不同类型的锁操作
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Getter
@AllArgsConstructor
public enum LockActionEnum {

    /**
     * 用户通用操作
     */
    USER_OPERATION("user", "用户操作"),

    /**
     * 团队通用操作
     */
    TEAM_OPERATION("team", "团队操作"),

    /**
     * 水印模板编辑
     */
    TEMPLATE_EDIT("template:edit", "编辑水印模板"),

    /**
     * 水印模板提交
     */
    TEMPLATE_SUBMIT("template:submit", "提交水印模板"),

    /**
     * 字体上传
     */
    FONT_UPLOAD("font:upload", "上传字体"),

    /**
     * 草稿保存
     */
    DRAFT_SAVE("draft:save", "保存草稿");

    private final String code;
    private final String desc;

    /**
     * 获取锁的Key
     *
     * @param action 动作类型
     * @param id     对象ID
     * @return 锁的Key
     */
    public static String getLockKey(LockActionEnum action, Object id) {
        return String.format("lock:%s:%s", action.getCode(), id);
    }
}
