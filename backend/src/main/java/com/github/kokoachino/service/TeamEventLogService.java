package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.TeamEventLogQueryDTO;
import com.github.kokoachino.model.dto.TeamEventLogRecordDTO;
import com.github.kokoachino.model.vo.InviteRecordVO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.TeamEventLogVO;
import java.util.List;


/**
 * 团队变更日志服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
public interface TeamEventLogService {

    void record(TeamEventLogRecordDTO dto);

    PageVO<TeamEventLogVO> queryLogs(TeamEventLogQueryDTO dto);

    List<String> listUsernames(String field, String keyword);

    List<InviteRecordVO> getInviteRecords(Integer codeId);
}
