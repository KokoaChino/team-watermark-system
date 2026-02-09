package com.github.kokoachino.model.vo;

import com.github.kokoachino.model.dto.watermark.WatermarkConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 草稿 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Builder
@Schema(description = "草稿信息")
public class DraftVO {

    @Schema(description = "草稿ID")
    private Integer id;

    @Schema(description = "源模板ID（为空表示新建）")
    private Integer sourceTemplateId;

    @Schema(description = "源模板版本号")
    private Integer sourceVersion;

    @Schema(description = "草稿名称")
    private String name;

    @Schema(description = "水印配置")
    private WatermarkConfigDTO config;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "是否存在冲突")
    private Boolean hasConflict;

    @Schema(description = "冲突信息")
    private String conflictMessage;
}
