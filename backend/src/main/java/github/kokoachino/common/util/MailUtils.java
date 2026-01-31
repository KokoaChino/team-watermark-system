package github.kokoachino.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


/**
 * 邮件工具类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailUtils {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.from}")
    private String from;

    public void sendSimpleMail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功： to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败： to={}, subject={}, error={}", to, subject, e.getMessage());
            throw new RuntimeException("邮件发送失败");
        }
    }
}
