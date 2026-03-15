package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 批量任务 VO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
@Schema(description = "批量任务信息")
public class BatchTaskVO {

    @Schema(description = "任务ID", example = "1")
    private Integer id;

    @Schema(description = "任务编号", example = "TSK12345-20260309-ABCDEFGH")
    private String taskNo;

    @Schema(description = "任务总数量", example = "50")
    private Integer totalCount;

    @Schema(description = "模板ID", example = "1")
    private Integer templateId;

    @Schema(description = "模板名称", example = "商品主图模板")
    private String templateName;

    @Schema(description = "创建时间", example = "2026-03-09T14:30:00")
    private LocalDateTime createdAt;
}
