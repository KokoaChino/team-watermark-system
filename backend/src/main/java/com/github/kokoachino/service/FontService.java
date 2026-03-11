package com.github.kokoachino.service;

import com.github.kokoachino.model.vo.FontVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


/**
 * 字体服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
public interface FontService {

    List<FontVO> getAvailableFonts(Integer teamId, String name);

    FontVO uploadFont(Integer teamId, Integer userId, String username, String name, MultipartFile fontFile);

    void deleteFont(Integer fontId, Integer teamId, Integer operatorUserId, String operatorUsername, boolean isLeader);

    boolean fontExists(String name, Integer teamId);
}
