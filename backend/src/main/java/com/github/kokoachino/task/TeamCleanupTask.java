package com.github.kokoachino.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.kokoachino.mapper.TeamMapper;
import com.github.kokoachino.mapper.TeamMemberMapper;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.entity.TeamMember;
import com.github.kokoachino.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;


/**
 * 团队清理定时任务
 * 清理条件：团队成员为0且归属用户已注销
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamCleanupTask {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupEmptyTeams() {
        log.info("开始执行团队清理任务...");
        List<Team> allTeams = teamMapper.selectList(null);
        int cleanedCount = 0;
        for (Team team : allTeams) {
            long memberCount = teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getTeamId, team.getId())
            );
            if (memberCount == 0) {
                User owner = userMapper.selectById(team.getOwnerId());
                if (owner == null) {
                    teamMapper.deleteById(team.getId());
                    cleanedCount++;
                    log.info("清理空团队：teamId={}, teamName={}, ownerId={}", team.getId(), team.getName(), team.getOwnerId());
                }
            }
        }
        log.info("团队清理任务完成，共清理 {} 个团队", cleanedCount);
    }
}
