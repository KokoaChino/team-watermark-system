package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.dto.log.OperationLogQueryDTO;
import com.github.kokoachino.model.enums.EventType;
import com.github.kokoachino.model.vo.OperationLogVO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 操作日志控制器
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@RestController
@RequestMapping("/api/operation-log")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作事件日志查询接口")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @PostMapping("/query")
    @Operation(summary = "查询团队操作日志", description = "分页查询本团队的操作日志，支持按事件类型、时间范围、操作人筛选")
    public Result<PageVO<OperationLogVO>> queryLogs(@RequestBody OperationLogQueryDTO dto) {
        PageVO<OperationLogVO> result = operationLogService.queryTeamLogs(dto);
        return Result.success(result);
    }

    @GetMapping("/event-types")
    @Operation(summary = "获取事件类型列表", description = "获取所有支持的事件类型，用于筛选")
    public Result<List<Map<String, String>>> getEventTypes() {
        List<Map<String, String>> types = Arrays.stream(EventType.values())
                .map(type -> Map.of(
                        "code", type.getCode(),
                        "description", type.getDescription(),
                        "category", type.getCategory()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }
}
