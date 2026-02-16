package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.vo.PointBalanceVO;
import com.github.kokoachino.service.PointService;
import com.github.kokoachino.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 点数控制器
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
@Tag(name = "点数管理", description = "团队点数相关接口")
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    @Operation(summary = "获取点数余额", description = "获取当前团队点数余额")
    public Result<PointBalanceVO> getBalance() {
        Integer teamId = UserContext.getUser().getTeamId();
        PointBalanceVO vo = pointService.getBalance(teamId);
        return Result.success(vo);
    }
}
