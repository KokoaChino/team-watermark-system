package com.github.kokoachino.model.vo;

import com.github.kokoachino.model.dto.watermark.WatermarkConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 水印模板 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Builder
@Schema(description = "水印模板信息")
public class WatermarkTemplateVO {

    @Schema(description = "模板ID")
    private Integer id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "团队ID")
    private Integer teamId;

    @Schema(description = "水印配置")
    private WatermarkConfigDTO config;

    @Schema(description = "创建人ID")
    private Integer createdById;

    @Schema(description = "创建人用户名")
    private String createdByUsername;

    @Schema(description = "版本号（乐观锁）")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
