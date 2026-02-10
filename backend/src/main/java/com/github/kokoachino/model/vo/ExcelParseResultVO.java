package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;


/**
 * Excel 解析结果 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Builder
@Schema(description = "Excel解析结果")
public class ExcelParseResultVO {

    @Schema(description = "表头列表", example = "[\"图片ID\", \"文字1\", \"文字2\", \"输出文件名\"]")
    private List<String> headers;

    @Schema(description = "图片ID到配置的映射（按ID映射模式）")
    private Map<String, ImageConfigVO> configById;

    @Schema(description = "按顺序的配置列表（按顺序映射模式）")
    private List<ImageConfigVO> configByOrder;

    @Schema(description = "总行数（不含表头）", example = "50")
    private Integer totalRows;

    /**
     * 图片配置 VO
     */
    @Data
    @Builder
    @Schema(description = "图片配置信息")
    public static class ImageConfigVO {

        @Schema(description = "图片ID", example = "img001")
        private String imageId;

        @Schema(description = "文字水印替换内容（key：水印索引或标识, value：替换文字）")
        private Map<String, String> textReplacements;

        @Schema(description = "图片水印替换链接（key：水印索引或标识, value：图片URL）")
        private Map<String, String> imageReplacements;

        @Schema(description = "输出文件名", example = "product_001_watermarked.jpg")
        private String outputFilename;

        @Schema(description = "输出路径", example = "/output/2026/02/")
        private String outputPath;

        @Schema(description = "原始行数据")
        private List<String> rawData;
    }
}
