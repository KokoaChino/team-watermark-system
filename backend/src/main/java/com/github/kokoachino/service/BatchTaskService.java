package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.SubmitBatchTaskDTO;
import com.github.kokoachino.model.vo.BatchTaskVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 批量任务服务接口（简化版）
 * 前端负责执行任务，后端只负责点数管理和结算
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
public interface BatchTaskService {

    /**
     * 提交批量任务
     * 预扣点数，确保用户同一时刻只有一个进行中的任务
     *
     * @param dto 任务信息
     * @return 任务VO
     */
    BatchTaskVO submitTask(SubmitBatchTaskDTO dto);

    /**
     * 完成任务
     * 接收结果ZIP文件存储到MinIO并设置过期时间，根据报表结算点数
     *
     * @param taskId       任务ID
     * @param successCount 成功处理数量
     * @param resultZip    结果ZIP文件
     * @param reportJson   处理报表（JSON格式）
     */
    void completeTask(Integer taskId, Integer successCount, MultipartFile resultZip, String reportJson);
}
