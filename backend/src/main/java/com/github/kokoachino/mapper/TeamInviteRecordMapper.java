package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.TeamInviteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 团队邀请记录 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Mapper
public interface TeamInviteRecordMapper extends BaseMapper<TeamInviteRecord> {

    /**
     * 查询邀请码的邀请记录列表
     *
     * @param inviteCodeId 邀请码ID
     * @return 邀请记录列表
     */
    List<TeamInviteRecord> selectByInviteCodeId(@Param("inviteCodeId") Integer inviteCodeId);
}
