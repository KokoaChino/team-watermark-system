package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 水印资源事件类型枚举
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Getter
public enum WatermarkResourceEventTypeEnum {

    TEMPLATE_CREATE("template_create", "模板创建"),
    TEMPLATE_UPDATE("template_update", "模板修改"),
    TEMPLATE_DELETE("template_delete", "模板删除"),
    FONT_UPLOAD("font_upload", "字体上传"),
    FONT_DELETE("font_delete", "字体删除");

    private final String value;
    private final String description;

    WatermarkResourceEventTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static WatermarkResourceEventTypeEnum fromValue(String value) {
        for (WatermarkResourceEventTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
