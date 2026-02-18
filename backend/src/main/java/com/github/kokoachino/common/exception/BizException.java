package com.github.kokoachino.common.exception;

import com.github.kokoachino.common.result.ResultCode;
import lombok.Getter;


/**
 * 自定义业务异常类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Getter
public class BizException extends RuntimeException {

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
