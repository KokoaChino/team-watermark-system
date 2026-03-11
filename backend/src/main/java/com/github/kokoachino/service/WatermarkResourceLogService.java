package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.WatermarkResourceLogQueryDTO;
import com.github.kokoachino.model.dto.WatermarkResourceLogRecordDTO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.WatermarkResourceLogVO;
import java.util.List;


/**
 * 水印资源日志服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
public interface WatermarkResourceLogService {

    void record(WatermarkResourceLogRecordDTO dto);

    PageVO<WatermarkResourceLogVO> queryLogs(WatermarkResourceLogQueryDTO dto);

    List<String> listOperatorUsernames(String keyword);
}
