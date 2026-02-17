package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * Excel表头枚举
 *
 * @author Kokoa_Chino
 * @date 2026-02-17
 */
@Getter
public enum HeaderEnum {

    ID("id", "图片ID", false),
    TEXT_WATERMARK("文字水印", "文字水印内容", true),
    IMAGE_WATERMARK("图片水印", "图片水印URL", true),
    FILE_PATH("文件路径", "输出路径", true),
    RENAME("重命名", "输出文件名", false),
    EXTENSION("拓展名", "输出格式转换", false);

    private final String value;
    private final String description;
    private final boolean multiColumn;

    HeaderEnum(String value, String description, boolean multiColumn) {
        this.value = value;
        this.description = description;
        this.multiColumn = multiColumn;
    }

    public static HeaderEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        String lowerValue = value.toLowerCase().trim();
        for (HeaderEnum header : values()) {
            if (header.value.equals(lowerValue)) {
                return header;
            }
        }
        return null;
    }
}
