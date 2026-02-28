package com.github.kokoachino.service;

import com.github.kokoachino.model.vo.FontVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


/**
 * 字体服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
public interface FontService {

    /**
     * 获取团队可用字体列表
     *
     * @param teamId 团队 ID
     * @param name   字体名称（模糊匹配）
     * @return 字体列表
     */
    List<FontVO> getAvailableFonts(Integer teamId, String name);

    /**
     * 上传字体
     *
     * @param teamId     团队ID
     * @param userId     用户ID
     * @param name       字体名称
     * @param fontFile   字体文件
     * @return 字体信息
     */
    FontVO uploadFont(Integer teamId, Integer userId, String name, MultipartFile fontFile);

    /**
     * 删除字体
     *
     * @param fontId     字体ID
     * @param teamId     团队ID
     * @param isLeader   是否是队长
     */
    void deleteFont(Integer fontId, Integer teamId, boolean isLeader);

    /**
     * 检查字体是否存在
     *
     * @param name   字体名称
     * @param teamId 团队ID
     * @return 是否存在
     */
    boolean fontExists(String name, Integer teamId);
}
