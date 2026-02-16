package com.github.kokoachino.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.vo.*;
import java.util.List;


/**
 * 团队服务接口
 *
 * @author kokoachino
 * @date 2026-01-31
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建个人团队
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param initialPoints 初始点数
     * @return 团队ID
     */
    Integer createPersonalTeam(Integer userId, String username, Integer initialPoints);

    /**
     * 生成团队邀请码
     *
     * @param teamId 团队ID
     * @param dto 生成邀请码参数
     * @param username 队长用户名
     * @return 邀请码信息
     */
    InviteCodeVO generateInviteCode(Integer teamId, GenerateInviteCodeDTO dto, String username);

    /**
     * 使用邀请码加入团队
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param dto 加入团队参数
     * @return 加入后的团队信息
     */
    TeamMemberVO joinTeam(Integer userId, String username, JoinTeamDTO dto);

    /**
     * 使邀请码失效
     *
     * @param codeId 邀请码ID
     * @param teamId 团队ID
     */
    void deactivateInviteCode(Integer codeId, Integer teamId);

    /**
     * 获取团队的邀请码列表
     *
     * @param teamId 团队ID
     * @return 邀请码列表
     */
    List<InviteCodeVO> getInviteCodesByTeamId(Integer teamId);

    /**
     * 获取邀请码的邀请记录
     *
     * @param codeId 邀请码ID
     * @return 邀请记录列表
     */
    List<InviteRecordVO> getInviteRecords(Integer codeId);

    /**
     * 退出团队
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return 新创建的个人团队信息
     */
    TeamMemberVO leaveTeam(Integer userId, String username);

    /**
     * 踢出团队成员
     *
     * @param teamId 团队ID
     * @param targetUserId 要踢出的用户ID
     * @param operatorUsername 操作人用户名
     */
    void kickMember(Integer teamId, Integer targetUserId, String operatorUsername);

    /**
     * 获取当前用户所属团队信息
     *
     * @param userId 用户ID
     * @return 团队成员信息
     */
    TeamMemberVO getCurrentTeamInfo(Integer userId);

    /**
     * 获取用户当前团队ID
     *
     * @param userId 用户ID
     * @return 团队ID
     */
    Integer getCurrentTeamId(Integer userId);

    /**
     * 判断用户是否是团队队长
     *
     * @param userId 用户ID
     * @param teamId 团队ID
     * @return 是否是队长
     */
    boolean isTeamLeader(Integer userId, Integer teamId);

    /**
     * 修改团队名称
     *
     * @param teamId 团队ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @param dto 修改团队名称参数
     * @return 更新后的团队信息
     */
    TeamMemberVO updateTeamName(Integer teamId, Integer userId, String username, UpdateTeamNameDTO dto);

    /**
     * 转让队长身份
     *
     * @param teamId 团队ID
     * @param currentLeaderId 当前队长ID
     * @param username 当前队长用户名
     * @param dto 转让队长参数
     * @return 更新后的团队信息
     */
    TeamMemberVO transferLeader(Integer teamId, Integer currentLeaderId, String username, TransferLeaderDTO dto);
}
