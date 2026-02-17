package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;


/**
 * Excel解析结果 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
@Data
@Builder
@Schema(description = "Excel解析结果")
public class ExcelParseResultVO {

    @Schema(description = "图片配置列表")
    private List<ImageConfigVO> configs;

    @Schema(description = "有效数据行数", example = "50")
    private Integer validRowCount;

    /**
     * 图片配置 VO
     */
    @Data
    @Builder
    @Schema(description = "图片配置信息")
    public static class ImageConfigVO {

        @Schema(description = "图片ID", example = "img001")
        private String imageId;

        @Schema(description = "文字水印内容列表（按表头顺序）", example = "[\"水印文字1\", \"水印文字2\"]")
        private List<String> textWatermarks;

        @Schema(description = "图片水印内容列表（按表头顺序）", example = "[\"https://example.com/watermark1.png\"]")
        private List<String> imageWatermarks;

        @Schema(description = "文件路径列表（按表头顺序）", example = "[\"output/2026/\", \"products/\"]")
        private List<String> filePaths;

        @Schema(description = "重命名", example = "product_001")
        private String rename;

        @Schema(description = "扩展名", example = "png")
        private String extension;
    }
}
