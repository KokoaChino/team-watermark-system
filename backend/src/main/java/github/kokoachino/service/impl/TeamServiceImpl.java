package github.kokoachino.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import github.kokoachino.mapper.TeamMapper;
import github.kokoachino.mapper.TeamMemberMapper;
import github.kokoachino.model.entity.Team;
import github.kokoachino.model.entity.TeamMember;
import github.kokoachino.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 团队服务实现类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Service
@RequiredArgsConstructor
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    private final TeamMemberMapper teamMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createPersonalTeam(Integer userId, String username, Integer initialPoints) {
        // 1. 创建团队
        Team team = new Team();
        team.setName(username + "的个人团队");
        team.setPointBalance(initialPoints); // 初始点数
        team.setLeaderId(userId);
        this.save(team);

        // 2. 添加成员关系
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole("leader");
        teamMemberMapper.insert(member);

        return team.getId();
    }
}
