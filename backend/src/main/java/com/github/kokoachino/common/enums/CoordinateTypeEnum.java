package com.github.kokoachino.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;


/**
 * 坐标类型枚举
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Getter
@Schema(description = "坐标类型")
public enum CoordinateTypeEnum {

    @Schema(description = "绝对坐标（像素）")
    ABSOLUTE("absolute", "绝对坐标"),

    @Schema(description = "百分比坐标")
    PERCENTAGE("percentage", "百分比坐标");

    private final String code;
    private final String description;

    CoordinateTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 将百分比坐标转换为绝对坐标
     *
     * @param percentageValue 百分比值 (0-100)
     * @param totalSize 总尺寸（像素）
     * @return 绝对坐标（像素）
     */
    public static int percentageToAbsolute(double percentageValue, int totalSize) {
        return (int) Math.round(percentageValue / 100.0 * totalSize);
    }

    /**
     * 将绝对坐标转换为百分比坐标
     *
     * @param absoluteValue 绝对坐标（像素）
     * @param totalSize 总尺寸（像素）
     * @return 百分比值 (0-100)
     */
    public static double absoluteToPercentage(int absoluteValue, int totalSize) {
        if (totalSize == 0) {
            return 0.0;
        }
        return (double) absoluteValue / totalSize * 100.0;
    }
}
