package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 操作事件日志 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 查询团队的操作日志
     */
    List<OperationLog> selectByTeamId(@Param("teamId") Integer teamId, @Param("limit") Integer limit);

    /**
     * 按条件查询日志
     */
    List<OperationLog> selectByConditions(@Param("teamId") Integer teamId,
                                          @Param("eventType") String eventType,
                                          @Param("userId") Integer userId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("offset") Integer offset,
                                          @Param("size") Integer size);

    /**
     * 统计日志数量
     */
    Long countByConditions(@Param("teamId") Integer teamId,
                           @Param("eventType") String eventType,
                           @Param("userId") Integer userId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);
}
