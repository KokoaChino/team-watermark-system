package com.github.kokoachino.common.mq;

import com.github.kokoachino.common.util.MailUtils;
import com.github.kokoachino.config.RabbitConfig;
import com.github.kokoachino.model.dto.MailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailConsumer {

    private final MailUtils mailUtils;

    @RabbitListener(queues = RabbitConfig.MAIL_QUEUE)
    public void consumeMailMessage(MailMessage message) {
        log.info("收到邮件消息：{}", message);
        try {
            if (message.isHtml()) {
                mailUtils.sendHtmlMail(message.getTo(), message.getSubject(), message.getContent());
            } else {
                mailUtils.sendSimpleMail(message.getTo(), message.getSubject(), message.getContent());
            }
        } catch (Exception e) {
            log.error("邮件发送失败：", e);
        }
    }
}
