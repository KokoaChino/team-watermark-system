package com.github.kokoachino.model.vo;

import com.github.kokoachino.model.dto.WatermarkConfigDTO;
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
@Schema(description = "水印模板草稿信息")
public class DraftVO {

    @Schema(description = "草稿ID", example = "1")
    private Integer id;

    @Schema(description = "源模板ID（为空表示新建）", example = "10")
    private Integer sourceTemplateId;

    @Schema(description = "源模板版本号", example = "3")
    private Integer sourceVersion;

    @Schema(description = "草稿名称", example = "商品主图水印")
    private String name;

    @Schema(description = "草稿配置")
    private WatermarkConfigDTO config;

    @Schema(description = "创建时间", example = "2026-02-10T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2026-02-10T15:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "是否有冲突", example = "false")
    private Boolean hasConflict;

    @Schema(description = "冲突信息", example = "模板已被其他人修改")
    private String conflictMessage;
}
