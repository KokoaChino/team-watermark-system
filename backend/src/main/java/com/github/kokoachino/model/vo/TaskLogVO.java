package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


/**
 * 批量任务日志 VO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量任务日志")
public class TaskLogVO {

    private Integer id;
    private String taskNo;
    private Integer createdById;
    private String createdByUsername;
    private String userStatus;
    private String userStatusDesc;
    private Integer templateId;
    private String templateName;
    private Integer templateVersion;
    private String templateSnapshot;
    private String description;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Long totalDurationMs;
    private Long totalSize;
    private String resultZipKey;
    private String report;
    private Integer deductedPoints;
    private Integer consumedPoints;
    private Integer refundedPoints;
    private String status;
    private String statusDesc;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
