package com.github.kokoachino.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.kokoachino.model.dto.GenerateInviteCodeDTO;
import com.github.kokoachino.model.dto.JoinTeamDTO;
import com.github.kokoachino.model.dto.TransferLeaderDTO;
import com.github.kokoachino.model.dto.UpdateTeamNameDTO;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.vo.InviteCodeVO;
import com.github.kokoachino.model.vo.InviteRecordVO;
import com.github.kokoachino.model.vo.TeamMemberVO;
import java.util.List;


/**
 * 团队服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
public interface TeamService extends IService<Team> {

    Integer createPersonalTeam(Integer userId, String username, Integer initialPoints);

    InviteCodeVO generateInviteCode(Integer teamId, Integer userId, String username, GenerateInviteCodeDTO dto);

    TeamMemberVO joinTeam(Integer userId, String username, JoinTeamDTO dto);

    void deactivateInviteCode(Integer codeId, Integer teamId, Integer operatorUserId, String operatorUsername);

    List<InviteCodeVO> getInviteCodesByTeamId(Integer teamId);

    List<InviteRecordVO> getInviteRecords(Integer codeId);

    TeamMemberVO leaveTeam(Integer userId, String username);

    void kickMember(Integer teamId, Integer operatorUserId, String operatorUsername, Integer targetUserId);

    TeamMemberVO getCurrentTeamInfo(Integer userId);

    Integer getCurrentTeamId(Integer userId);

    boolean isTeamLeader(Integer userId, Integer teamId);

    TeamMemberVO updateTeamName(Integer teamId, Integer userId, String username, UpdateTeamNameDTO dto);

    TeamMemberVO transferLeader(Integer teamId, Integer currentLeaderId, String username, TransferLeaderDTO dto);
}
