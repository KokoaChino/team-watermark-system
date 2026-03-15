package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.TeamEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 团队变更日志 Mapper
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Mapper
public interface TeamEventLogMapper extends BaseMapper<TeamEventLog> {

    List<TeamEventLog> selectByConditions(@Param("teamId") Integer teamId,
                                          @Param("eventType") String eventType,
                                          @Param("operatorKeyword") String operatorKeyword,
                                          @Param("affectedKeyword") String affectedKeyword,
                                          @Param("inviteCode") String inviteCode,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("offset") Integer offset,
                                          @Param("size") Integer size);

    Long countByConditions(@Param("teamId") Integer teamId,
                           @Param("eventType") String eventType,
                           @Param("operatorKeyword") String operatorKeyword,
                           @Param("affectedKeyword") String affectedKeyword,
                           @Param("inviteCode") String inviteCode,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    List<String> selectDistinctOperatorUsernames(@Param("teamId") Integer teamId,
                                                 @Param("keyword") String keyword);

    List<String> selectDistinctAffectedUsernames(@Param("teamId") Integer teamId,
                                                 @Param("keyword") String keyword);

    List<TeamEventLog> selectInviteRecordsByInviteCodeId(@Param("teamId") Integer teamId,
                                                         @Param("inviteCodeId") Integer inviteCodeId);
}
