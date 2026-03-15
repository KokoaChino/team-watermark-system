package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.SubmitBatchTaskDTO;
import com.github.kokoachino.model.dto.TaskLogQueryDTO;
import com.github.kokoachino.model.vo.BatchTaskVO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.TaskLogVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


/**
 * 批量任务服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
public interface BatchTaskService {

    BatchTaskVO submitTask(SubmitBatchTaskDTO dto);

    void completeTask(Integer taskId, Integer successCount, MultipartFile resultZip, String reportJson);

    PageVO<TaskLogVO> queryTaskLogs(TaskLogQueryDTO dto);

    List<String> listTaskUsernames(String keyword);
}
