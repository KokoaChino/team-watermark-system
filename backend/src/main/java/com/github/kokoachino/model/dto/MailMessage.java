package com.github.kokoachino.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serial;
import java.io.Serializable;


/**
 * 邮件消息 DTO
 * 实现 Serializable 接口用于 RabbitMQ 消息队列传输
 * RabbitMQ 需要将对象序列化后才能在网络中传输
 *
 * @author kokoachino
 * @date 2026-02-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailMessage implements Serializable {

    /**
     * 序列化版本号，用于保证反序列化兼容性
     * 建议修改类结构时同步更新此版本号
     */
    @Serial
    private static final long serialVersionUID = 1L;

    private String to;
    private String subject;
    private String content;
    private boolean isHtml;
}
