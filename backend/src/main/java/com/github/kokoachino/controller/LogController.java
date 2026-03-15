package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.dto.PointChangeLogQueryDTO;
import com.github.kokoachino.model.dto.TaskLogQueryDTO;
import com.github.kokoachino.model.dto.TeamEventLogQueryDTO;
import com.github.kokoachino.model.dto.WatermarkResourceLogQueryDTO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.PointChangeLogVO;
import com.github.kokoachino.model.vo.TaskLogVO;
import com.github.kokoachino.model.vo.TeamEventLogVO;
import com.github.kokoachino.model.vo.WatermarkResourceLogVO;
import com.github.kokoachino.service.BatchTaskService;
import com.github.kokoachino.service.PointChangeLogService;
import com.github.kokoachino.service.TeamEventLogService;
import com.github.kokoachino.service.WatermarkResourceLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


/**
 * 日志查询控制器
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "日志查询", description = "团队变更、水印资源、点数流水和任务记录查询接口")
public class LogController {

    private final TeamEventLogService teamEventLogService;
    private final WatermarkResourceLogService watermarkResourceLogService;
    private final PointChangeLogService pointChangeLogService;
    private final BatchTaskService batchTaskService;

    @PostMapping("/team/query")
    @Operation(summary = "查询团队变更日志")
    public Result<PageVO<TeamEventLogVO>> queryTeamLogs(@RequestBody TeamEventLogQueryDTO dto) {
        return Result.success(teamEventLogService.queryLogs(dto));
    }

    @GetMapping("/team/usernames")
    @Operation(summary = "查询团队变更日志用户名候选项")
    public Result<List<String>> queryTeamLogUsernames(@RequestParam(defaultValue = "operator") String field,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(teamEventLogService.listUsernames(field, keyword));
    }

    @PostMapping("/watermark/query")
    @Operation(summary = "查询水印资源日志")
    public Result<PageVO<WatermarkResourceLogVO>> queryWatermarkLogs(@RequestBody WatermarkResourceLogQueryDTO dto) {
        return Result.success(watermarkResourceLogService.queryLogs(dto));
    }

    @GetMapping("/watermark/usernames")
    @Operation(summary = "查询水印资源日志用户名候选项")
    public Result<List<String>> queryWatermarkLogUsernames(@RequestParam(required = false) String keyword) {
        return Result.success(watermarkResourceLogService.listOperatorUsernames(keyword));
    }

    @PostMapping("/points/query")
    @Operation(summary = "查询点数流水日志")
    public Result<PageVO<PointChangeLogVO>> queryPointLogs(@RequestBody PointChangeLogQueryDTO dto) {
        return Result.success(pointChangeLogService.queryLogs(dto));
    }

    @GetMapping("/points/usernames")
    @Operation(summary = "查询点数流水日志用户名候选项")
    public Result<List<String>> queryPointLogUsernames(@RequestParam(required = false) String keyword) {
        return Result.success(pointChangeLogService.listOperatorUsernames(keyword));
    }

    @PostMapping("/tasks/query")
    @Operation(summary = "查询任务记录日志")
    public Result<PageVO<TaskLogVO>> queryTaskLogs(@RequestBody TaskLogQueryDTO dto) {
        return Result.success(batchTaskService.queryTaskLogs(dto));
    }

    @GetMapping("/tasks/usernames")
    @Operation(summary = "查询任务记录日志用户名候选项")
    public Result<List<String>> queryTaskLogUsernames(@RequestParam(required = false) String keyword) {
        return Result.success(batchTaskService.listTaskUsernames(keyword));
    }
}
