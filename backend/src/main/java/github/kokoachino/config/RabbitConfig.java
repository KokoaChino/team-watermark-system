package github.kokoachino.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Configuration
public class RabbitConfig {
    public static final String MAIL_QUEUE = "mail.queue";

    @Bean
    public Queue mailQueue() {
        return new Queue(MAIL_QUEUE, true);
    }
}
