package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.PointChangeLogQueryDTO;
import com.github.kokoachino.model.dto.PointChangeLogRecordDTO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.PointChangeLogVO;
import java.util.List;


/**
 * 点数流水日志服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
public interface PointChangeLogService {

    void record(PointChangeLogRecordDTO dto);

    PageVO<PointChangeLogVO> queryLogs(PointChangeLogQueryDTO dto);

    List<String> listOperatorUsernames(String keyword);
}
