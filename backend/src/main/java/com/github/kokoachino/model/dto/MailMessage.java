package com.github.kokoachino.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailMessage implements Serializable {
    private String to;
    private String subject;
    private String content;
    private boolean isHtml;
}
