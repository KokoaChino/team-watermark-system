package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.WatermarkResourceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 水印资源日志 Mapper
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Mapper
public interface WatermarkResourceLogMapper extends BaseMapper<WatermarkResourceLog> {

    List<WatermarkResourceLog> selectByConditions(@Param("teamId") Integer teamId,
                                                  @Param("eventType") String eventType,
                                                  @Param("resourceScope") String resourceScope,
                                                  @Param("resourceName") String resourceName,
                                                  @Param("operatorKeyword") String operatorKeyword,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  @Param("offset") Integer offset,
                                                  @Param("size") Integer size);

    Long countByConditions(@Param("teamId") Integer teamId,
                           @Param("eventType") String eventType,
                           @Param("resourceScope") String resourceScope,
                           @Param("resourceName") String resourceName,
                           @Param("operatorKeyword") String operatorKeyword,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    List<String> selectDistinctOperatorUsernames(@Param("teamId") Integer teamId,
                                                 @Param("keyword") String keyword);
}
