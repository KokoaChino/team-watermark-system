package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.CoordinateTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 位置配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "位置配置")
public class PositionDTO {

    @NotNull(message = "坐标类型不能为空")
    @Schema(description = "坐标类型：ABSOLUTE-绝对坐标（像素），PERCENTAGE-百分比坐标", example = "PERCENTAGE")
    private CoordinateTypeEnum coordinateTypeEnum;

    @NotNull(message = "X坐标不能为空")
    @Schema(description = "X坐标：绝对坐标时为像素值，百分比坐标时为0-100", example = "50.0")
    private Double x;

    @NotNull(message = "Y坐标不能为空")
    @Schema(description = "Y坐标：绝对坐标时为像素值，百分比坐标时为0-100", example = "80.0")
    private Double y;

    /**
     * 将当前坐标转换为绝对坐标（像素）
     *
     * @param imageWidth  底图宽度
     * @param imageHeight 底图高度
     * @return 转换后的绝对坐标 [x, y]
     */
    public int[] toAbsolute(int imageWidth, int imageHeight) {
        if (coordinateTypeEnum == CoordinateTypeEnum.ABSOLUTE) {
            return new int[]{x.intValue(), y.intValue()};
        } else {
            return new int[]{
                CoordinateTypeEnum.percentageToAbsolute(x, imageWidth),
                CoordinateTypeEnum.percentageToAbsolute(y, imageHeight)
            };
        }
    }

    /**
     * 将当前坐标转换为百分比坐标
     *
     * @param imageWidth  底图宽度
     * @param imageHeight 底图高度
     * @return 转换后的百分比坐标 [x, y]
     */
    public double[] toPercentage(int imageWidth, int imageHeight) {
        if (coordinateTypeEnum == CoordinateTypeEnum.PERCENTAGE) {
            return new double[]{x, y};
        } else {
            return new double[]{
                CoordinateTypeEnum.absoluteToPercentage(x.intValue(), imageWidth),
                CoordinateTypeEnum.absoluteToPercentage(y.intValue(), imageHeight)
            };
        }
    }
}
