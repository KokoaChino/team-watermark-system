package com.github.kokoachino.controller;

import com.github.kokoachino.common.enums.*;
import com.github.kokoachino.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 枚举控制器
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/api/enums")
@Tag(name = "枚举管理", description = "获取系统枚举值")
public class EnumController {

    @GetMapping("/event-types")
    @Operation(summary = "获取旧操作日志事件类型")
    public Result<List<Map<String, String>>> getEventTypes() {
        List<Map<String, String>> types = Arrays.stream(EventTypeEnum.values())
                .map(type -> Map.of(
                        "code", type.getCode(),
                        "description", type.getDescription(),
                        "category", type.getCategory()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/black-list-types")
    @Operation(summary = "获取黑名单类型")
    public Result<List<Map<String, String>>> getBlackListTypes() {
        List<Map<String, String>> types = Arrays.stream(BlackListTypeEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "desc", type.getDesc()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/coordinate-types")
    @Operation(summary = "获取坐标类型")
    public Result<List<Map<String, String>>> getCoordinateTypes() {
        List<Map<String, String>> types = Arrays.stream(CoordinateTypeEnum.values())
                .map(type -> Map.of(
                        "code", type.getCode(),
                        "description", type.getDescription()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/invite-code-statuses")
    @Operation(summary = "获取邀请码状态")
    public Result<List<Map<String, String>>> getInviteCodeStatuses() {
        List<Map<String, String>> types = Arrays.stream(InviteCodeStatusEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "desc", type.getDesc()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/login-types")
    @Operation(summary = "获取登录类型")
    public Result<List<Map<String, String>>> getLoginTypes() {
        List<Map<String, String>> types = Arrays.stream(LoginTypeEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "desc", type.getDesc()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/team-roles")
    @Operation(summary = "获取团队角色")
    public Result<List<Map<String, String>>> getTeamRoles() {
        List<Map<String, String>> types = Arrays.stream(TeamRoleEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "desc", type.getDesc()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/token-types")
    @Operation(summary = "获取令牌类型")
    public Result<List<Map<String, String>>> getTokenTypes() {
        List<Map<String, String>> types = Arrays.stream(TokenTypeEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "desc", type.getDesc()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/verification-code-types")
    @Operation(summary = "获取验证码类型")
    public Result<List<Map<String, String>>> getVerificationCodeTypes() {
        List<Map<String, String>> types = Arrays.stream(VerificationCodeTypeEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "desc", type.getDesc()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/mapping-modes")
    @Operation(summary = "获取 Excel 映射模式")
    public Result<List<Map<String, String>>> getMappingModes() {
        List<Map<String, String>> types = Arrays.stream(MappingModeEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "description", type.getDescription()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/duplicate-handlings")
    @Operation(summary = "获取重复 ID 处理策略")
    public Result<List<Map<String, String>>> getDuplicateHandlings() {
        List<Map<String, String>> types = Arrays.stream(DuplicateHandlingEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "description", type.getDescription()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/invalid-char-handlings")
    @Operation(summary = "获取异常字符处理策略")
    public Result<List<Map<String, String>>> getInvalidCharHandlings() {
        List<Map<String, String>> types = Arrays.stream(InvalidCharHandlingEnum.values())
                .map(type -> Map.of(
                        "value", type.getValue(),
                        "description", type.getDescription()
                ))
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/excel-headers")
    @Operation(summary = "获取 Excel 表头类型")
    public Result<List<Map<String, Object>>> getExcelHeaders() {
        List<Map<String, Object>> types = Arrays.stream(HeaderEnum.values())
                .map(type -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("value", type.getValue());
                    map.put("description", type.getDescription());
                    map.put("multiColumn", type.isMultiColumn());
                    return map;
                })
                .collect(Collectors.toList());
        return Result.success(types);
    }

    @GetMapping("/team-event-types")
    @Operation(summary = "获取团队变更事件类型")
    public Result<List<Map<String, String>>> getTeamEventTypes() {
        return Result.success(Arrays.stream(TeamEventTypeEnum.values())
                .map(type -> Map.of("value", type.getValue(), "desc", type.getDescription()))
                .toList());
    }

    @GetMapping("/watermark-log-event-types")
    @Operation(summary = "获取水印资源事件类型")
    public Result<List<Map<String, String>>> getWatermarkLogEventTypes() {
        return Result.success(Arrays.stream(WatermarkResourceEventTypeEnum.values())
                .map(type -> Map.of("value", type.getValue(), "desc", type.getDescription()))
                .toList());
    }

    @GetMapping("/watermark-resource-scopes")
    @Operation(summary = "获取水印资源范围")
    public Result<List<Map<String, String>>> getWatermarkResourceScopes() {
        return Result.success(Arrays.stream(WatermarkResourceScopeEnum.values())
                .map(type -> Map.of("value", type.getValue(), "desc", type.getDescription()))
                .toList());
    }

    @GetMapping("/point-change-types")
    @Operation(summary = "获取点数变动类型")
    public Result<List<Map<String, String>>> getPointChangeTypes() {
        return Result.success(Arrays.stream(PointChangeTypeEnum.values())
                .map(type -> Map.of("value", type.getValue(), "desc", type.getDescription()))
                .toList());
    }

    @GetMapping("/point-source-types")
    @Operation(summary = "获取点数来源类型")
    public Result<List<Map<String, String>>> getPointSourceTypes() {
        return Result.success(Arrays.stream(PointSourceTypeEnum.values())
                .map(type -> Map.of("value", type.getValue(), "desc", type.getDescription()))
                .toList());
    }

    @GetMapping("/task-log-statuses")
    @Operation(summary = "获取任务记录状态")
    public Result<List<Map<String, String>>> getTaskLogStatuses() {
        return Result.success(Arrays.stream(TaskLogStatusEnum.values())
                .map(type -> Map.of("value", type.getValue(), "desc", type.getDescription()))
                .toList());
    }

    @GetMapping("/log-user-statuses")
    @Operation(summary = "获取日志用户状态")
    public Result<List<Map<String, String>>> getLogUserStatuses() {
        return Result.success(Arrays.stream(LogUserStatusEnum.values())
                .map(type -> Map.of("value", type.getValue(), "desc", type.getDescription()))
                .toList());
    }
}