package com.github.kokoachino.model.dto.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;


/**
 * 完成任务 DTO（简化版）
 * 前端处理完成后调用，后端根据报表结算点数
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Schema(description = "完成任务请求")
public class CompleteTaskDTO {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID（submit接口返回）")
    private Integer taskId;

    @NotNull(message = "成功处理数量不能为空")
    @Min(value = 0, message = "成功数量不能为负数")
    @Schema(description = "成功处理的图片数量", example = "48")
    private Integer successCount;

    @Schema(description = "处理报表（包含成功/失败详情）")
    private List<TaskReportItemDTO> report;

    /**
     * 报表项 DTO
     */
    @Data
    @Schema(description = "任务报表项")
    public static class TaskReportItemDTO {

        @Schema(description = "图片ID或文件名")
        private String imageId;

        @Schema(description = "处理状态：success-成功，failed-失败")
        private String status;

        @Schema(description = "错误信息（失败时填写）")
        private String errorMessage;
    }
}
