package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.dto.SubmitBatchTaskDTO;
import com.github.kokoachino.model.vo.BatchTaskVO;
import com.github.kokoachino.service.BatchTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * 批量任务控制器
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/api/batch-task")
@RequiredArgsConstructor
@Tag(name = "批量任务管理", description = "批量水印任务相关接口")
public class BatchTaskController {

    private final BatchTaskService batchTaskService;

    @PostMapping("/submit")
    @Operation(summary = "提交批量任务", description = "提交批量任务，预扣点数并创建任务日志")
    public Result<BatchTaskVO> submitTask(@Valid @RequestBody SubmitBatchTaskDTO dto) {
        return Result.success(batchTaskService.submitTask(dto));
    }

    @PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "完成任务", description = "前端处理完成后调用，上传结果ZIP文件并结算点数")
    public Result<String> completeTask(
            @RequestParam @NotNull @Parameter(description = "任务ID") Integer taskId,
            @RequestParam @NotNull @Min(0) @Parameter(description = "成功处理数量") Integer successCount,
            @RequestParam(required = false) @Parameter(description = "结果ZIP文件") MultipartFile resultZip,
            @RequestParam(required = false) @Parameter(description = "处理报表（JSON格式）") String reportJson) {
        batchTaskService.completeTask(taskId, successCount, resultZip, reportJson);
        return Result.success("任务完成，点数已结算");
    }
}
