package com.github.kokoachino.service;

import com.github.kokoachino.common.enums.LogUserStatusEnum;


/**
 * 日志用户状态同步服务
 *
 * @author Kokoa_Chino
 * @date 2026-03-10
 */
public interface LogUserStatusSyncService {

    void syncUserStatus(Integer teamId, Integer userId, String username, LogUserStatusEnum targetStatus);
}
