package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * 文件控制器
 *
 * @author Kokoa_Chino
 * @date 2026-02-28
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传到 MinIO")
public class FileController {

    private final MinioService minioService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件到 MinIO", description = "上传文件并返回完整的文件访问 URL（可直接访问）")
    public Result<String> uploadFile(
            @Parameter(description = "文件") @RequestParam MultipartFile file,
            @Parameter(description = "对象 key 前缀（如 \"images/\"），可选，默认为 \"uploads/\"") @RequestParam(required = false, defaultValue = "uploads/") String prefix) {
        String fileUrl = minioService.uploadFileWithAutoKey(file, prefix);
        return Result.success(fileUrl);
    }
}
