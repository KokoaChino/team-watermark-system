package github.kokoachino.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.kokoachino.model.entity.Team;


/**
 * 团队服务接口
 *
 * @author kokoachino
 * @date 2026-01-31
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建个人团队
     * @param userId 用户ID
     * @param username 用户名
     * @param initialPoints 初始点数
     * @return 团队ID
     */
    Integer createPersonalTeam(Integer userId, String username, Integer initialPoints);
}
