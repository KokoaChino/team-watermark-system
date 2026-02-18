package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.model.dto.FontQueryDTO;
import com.github.kokoachino.model.vo.FontVO;
import com.github.kokoachino.service.FontService;
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

    @GetMapping("/list")
    @Operation(summary = "获取可用字体列表", description = "获取系统字体和团队上传的字体，支持条件筛选")
    public Result<List<FontVO>> getAvailableFonts(FontQueryDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        List<FontVO> list = fontService.getAvailableFonts(teamId, dto);
        return Result.success(list);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传字体", description = "上传团队自定义字体（.ttf或.otf格式）")
    public Result<FontVO> uploadFont(
            @Parameter(description = "字体名称") @RequestParam String name,
            @Parameter(description = "字体文件(.ttf或.otf)") @RequestParam MultipartFile fontFile) {
        Integer userId = UserContext.getUserId();
        Integer teamId = TeamContext.getTeamId();
        FontVO result = fontService.uploadFont(teamId, userId, name, fontFile);
        return Result.success(result);
    }

    @DeleteMapping("/{fontId}")
    @Operation(summary = "删除字体", description = "删除团队上传的字体（仅队长可操作）")
    public Result<Void> deleteFont(
            @Parameter(description = "字体ID") @PathVariable Integer fontId) {
        Integer teamId = TeamContext.getTeamId();
        boolean isLeader = TeamContext.isLeader();
        fontService.deleteFont(fontId, teamId, isLeader);
        return Result.success(null);
    }
}
