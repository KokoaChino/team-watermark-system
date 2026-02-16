package com.github.kokoachino.service;

import com.github.kokoachino.model.vo.PointBalanceVO;


/**
 * 点数服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
public interface PointService {

    /**
     * 获取团队点数余额
     *
     * @param teamId 团队ID
     * @return 点数余额VO
     */
    PointBalanceVO getBalance(Integer teamId);

    /**
     * 预扣点数
     *
     * @param teamId   团队ID
     * @param userId   用户ID
     * @param points   点数
     * @param bizType  业务类型
     * @param bizId    业务ID
     * @param description 描述
     * @return 是否成功
     */
    boolean deductPoints(Integer teamId, Integer userId, Integer points, String bizType, String bizId, String description);

    /**
     * 返还点数
     *
     * @param teamId   团队ID
     * @param userId   用户ID
     * @param points   点数
     * @param bizType  业务类型
     * @param bizId    业务ID
     * @param description 描述
     * @return 是否成功
     */
    boolean refundPoints(Integer teamId, Integer userId, Integer points, String bizType, String bizId, String description);

    /**
     * 充值点数
     *
     * @param teamId   团队ID
     * @param userId   用户ID
     * @param points   点数
     * @param bizType  业务类型
     * @param bizId    业务ID
     * @param description 描述
     * @return 是否成功
     */
    boolean rechargePoints(Integer teamId, Integer userId, Integer points, String bizType, String bizId, String description);

    /**
     * 检查点数是否充足
     *
     * @param teamId 团队ID
     * @param points 所需点数
     * @return 是否充足
     */
    boolean hasEnoughPoints(Integer teamId, Integer points);
}
