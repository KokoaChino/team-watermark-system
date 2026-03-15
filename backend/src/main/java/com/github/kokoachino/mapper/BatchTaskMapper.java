package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.BatchTask;
import com.github.kokoachino.model.vo.TaskLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 批量任务日志 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Mapper
public interface BatchTaskMapper extends BaseMapper<BatchTask> {

    int countUncompletedByUserId(@Param("userId") Integer userId);

    List<TaskLogVO> selectLogPage(@Param("teamId") Integer teamId,
                                  @Param("status") String status,
                                  @Param("operatorKeyword") String operatorKeyword,
                                  @Param("templateName") String templateName,
                                  @Param("taskNo") String taskNo,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  @Param("offset") Integer offset,
                                  @Param("size") Integer size);

    Long countLogPage(@Param("teamId") Integer teamId,
                      @Param("status") String status,
                      @Param("operatorKeyword") String operatorKeyword,
                      @Param("templateName") String templateName,
                      @Param("taskNo") String taskNo,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    List<String> selectDistinctCreatedByUsernames(@Param("teamId") Integer teamId,
                                                  @Param("keyword") String keyword);
}
