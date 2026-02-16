package com.github.kokoachino.common.util;

import com.github.kokoachino.model.vo.TeamMemberVO;


/**
 * 团队上下文
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
public class TeamContext {

    private static final ThreadLocal<TeamMemberVO> CONTEXT = new ThreadLocal<>();

    public static void setTeam(TeamMemberVO team) {
        CONTEXT.set(team);
    }

    public static TeamMemberVO getTeam() {
        return CONTEXT.get();
    }

    public static Integer getTeamId() {
        TeamMemberVO team = CONTEXT.get();
        return team != null ? team.getTeamId() : null;
    }

    public static String getRole() {
        TeamMemberVO team = CONTEXT.get();
        return team != null ? team.getRole() : null;
    }

    public static boolean isLeader() {
        TeamMemberVO team = CONTEXT.get();
        return team != null && "leader".equals(team.getRole());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
