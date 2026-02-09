package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.model.dto.CreateTemplateDTO;
import com.github.kokoachino.model.dto.SaveDraftDTO;
import com.github.kokoachino.model.dto.SubmitDraftDTO;
import com.github.kokoachino.model.dto.UpdateTemplateDTO;
import com.github.kokoachino.model.vo.DraftVO;
import com.github.kokoachino.model.vo.WatermarkTemplateVO;
import com.github.kokoachino.service.TeamService;
import com.github.kokoachino.service.WatermarkTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;


/**
 * 水印模板控制器
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
@Tag(name = "水印模板管理", description = "水印模板的CRUD和用户工作区管理")
public class WatermarkTemplateController {

    private final WatermarkTemplateService templateService;
    private final TeamService teamService;

    @GetMapping("/list")
    @Operation(summary = "获取团队模板列表")
    public Result<List<WatermarkTemplateVO>> getTemplateList() {
        Integer userId = UserContext.getUserId();
        Integer teamId = teamService.getCurrentTeamId(userId);
        List<WatermarkTemplateVO> list = templateService.getTemplateList(teamId);
        return Result.success(list);
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "获取模板详情")
    public Result<WatermarkTemplateVO> getTemplateDetail(
            @Parameter(description = "模板ID") @PathVariable Integer templateId) {
        WatermarkTemplateVO detail = templateService.getTemplateDetail(templateId);
        return Result.success(detail);
    }

    @PostMapping
    @Operation(summary = "创建模板（直接创建）")
    public Result<WatermarkTemplateVO> createTemplate(
            @Valid @RequestBody CreateTemplateDTO dto) {
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        Integer teamId = teamService.getCurrentTeamId(userId);
        WatermarkTemplateVO result = templateService.createTemplate(teamId, userId, username, dto);
        return Result.success(result);
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "更新模板")
    public Result<WatermarkTemplateVO> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Integer templateId,
            @Valid @RequestBody UpdateTemplateDTO dto) {
        WatermarkTemplateVO result = templateService.updateTemplate(templateId, dto);
        return Result.success(result);
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除模板")
    public Result<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Integer templateId) {
        Integer userId = UserContext.getUserId();
        Integer teamId = teamService.getCurrentTeamId(userId);
        boolean isLeader = teamService.isTeamLeader(userId, teamId);
        templateService.deleteTemplate(templateId, userId, teamId, isLeader);
        return Result.success(null);
    }

    @PostMapping("/draft/from-template/{templateId}")
    @Operation(summary = "基于现有模板创建草稿")
    public Result<DraftVO> createDraftFromTemplate(
            @Parameter(description = "源模板ID") @PathVariable Integer templateId) {
        Integer userId = UserContext.getUserId();
        DraftVO draft = templateService.createDraftFromTemplate(templateId, userId);
        return Result.success(draft);
    }

    @PostMapping("/draft/new")
    @Operation(summary = "创建空白草稿")
    public Result<DraftVO> createEmptyDraft() {
        Integer userId = UserContext.getUserId();
        DraftVO draft = templateService.createEmptyDraft(userId);
        return Result.success(draft);
    }

    @GetMapping("/draft/current")
    @Operation(summary = "获取当前用户的草稿")
    public Result<DraftVO> getCurrentDraft() {
        Integer userId = UserContext.getUserId();
        DraftVO draft = templateService.getCurrentDraft(userId);
        return Result.success(draft);
    }

    @PutMapping("/draft/save")
    @Operation(summary = "保存草稿")
    public Result<DraftVO> saveDraft(
            @Valid @RequestBody SaveDraftDTO dto) {
        Integer userId = UserContext.getUserId();
        DraftVO draft = templateService.saveDraft(userId, dto);
        return Result.success(draft);
    }

    @PostMapping("/draft/submit")
    @Operation(summary = "提交草稿")
    public Result<WatermarkTemplateVO> submitDraft(
            @Valid @RequestBody SubmitDraftDTO dto) {
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        Integer teamId = teamService.getCurrentTeamId(userId);
        WatermarkTemplateVO result = templateService.submitDraft(userId, username, teamId, dto);
        return Result.success(result);
    }

    @DeleteMapping("/draft/clear")
    @Operation(summary = "清空草稿")
    public Result<Void> clearDraft() {
        Integer userId = UserContext.getUserId();
        templateService.clearDraft(userId);
        return Result.success(null);
    }
}
