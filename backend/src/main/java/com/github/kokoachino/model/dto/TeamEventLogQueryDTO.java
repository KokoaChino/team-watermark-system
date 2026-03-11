package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 团队变更日志查询 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Schema(description = "团队变更日志查询条件")
public class TeamEventLogQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "事件类型", example = "member_join")
    private String eventType;

    @Schema(description = "操作人用户名关键字", example = "kokoa")
    private String operatorKeyword;

    @Schema(description = "影响成员用户名关键字", example = "member")
    private String affectedKeyword;

    @Schema(description = "邀请码关键字", example = "TEAM2026")
    private String inviteCode;

    @Schema(description = "开始时间", example = "2026-03-09T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-03-09T23:59:59")
    private LocalDateTime endTime;
}
