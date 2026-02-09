package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.model.vo.FontVO;
import com.github.kokoachino.service.FontService;
import com.github.kokoachino.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


/**
 * 字体控制器
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@RestController
@RequestMapping("/api/font")
@RequiredArgsConstructor
@Tag(name = "字体管理", description = "字体上传和管理")
public class FontController {

    private final FontService fontService;
    private final TeamService teamService;

    @GetMapping("/list")
    @Operation(summary = "获取可用字体列表")
    public Result<List<FontVO>> getAvailableFonts() {
        Integer userId = UserContext.getUserId();
        Integer teamId = teamService.getCurrentTeamId(userId);
        List<FontVO> list = fontService.getAvailableFonts(teamId);
        return Result.success(list);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传字体")
    public Result<FontVO> uploadFont(
            @Parameter(description = "字体名称") @RequestParam String name,
            @Parameter(description = "字体文件(.ttf或.otf)") @RequestParam MultipartFile fontFile) {
        Integer userId = UserContext.getUserId();
        Integer teamId = teamService.getCurrentTeamId(userId);
        FontVO result = fontService.uploadFont(teamId, userId, name, fontFile);
        return Result.success(result);
    }

    @DeleteMapping("/{fontId}")
    @Operation(summary = "删除字体")
    public Result<Void> deleteFont(
            @Parameter(description = "字体ID") @PathVariable Integer fontId) {
        Integer userId = UserContext.getUserId();
        Integer teamId = teamService.getCurrentTeamId(userId);
        boolean isLeader = teamService.isTeamLeader(userId, teamId);
        fontService.deleteFont(fontId, teamId, isLeader);
        return Result.success(null);
    }

    @GetMapping("/check")
    @Operation(summary = "检查字体名称是否已存在")
    public Result<Boolean> checkFontExists(
            @Parameter(description = "字体名称") @RequestParam String name) {
        Integer userId = UserContext.getUserId();
        Integer teamId = teamService.getCurrentTeamId(userId);
        boolean exists = fontService.fontExists(name, teamId);
        return Result.success(exists);
    }
}
