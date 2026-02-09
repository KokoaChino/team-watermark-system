package com.github.kokoachino.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 邀请码工具类测试
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
public class InviteCodeUtilsTest {

    @Test
    void generateRawCode_ShouldReturn6CharCode() {
        // Act
        String code = InviteCodeUtils.generateRawCode();

        // Assert
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(InviteCodeUtils.isValidCodeFormat(code));
    }

    @Test
    void generateRawCode_ShouldReturnUniqueCodes() {
        // Act
        String code1 = InviteCodeUtils.generateRawCode();
        String code2 = InviteCodeUtils.generateRawCode();

        // Assert
        assertNotEquals(code1, code2);
    }

    @Test
    void wrapCode_ShouldWrapWithBrackets() {
        // Arrange
        String rawCode = "ABCD23";

        // Act
        String wrapped = InviteCodeUtils.wrapCode(rawCode);

        // Assert
        assertEquals("【ABCD23】", wrapped);
    }

    @Test
    void generateShareText_ShouldContainTeamNameAndCode() {
        // Arrange
        String teamName = "测试团队";
        String rawCode = "ABCD23";

        // Act
        String shareText = InviteCodeUtils.generateShareText(teamName, rawCode);

        // Assert
        assertTrue(shareText.contains(teamName));
        assertTrue(shareText.contains("【ABCD23】"));
    }

    @Test
    void extractCodeFromText_ShouldExtractCodeFromBrackets() {
        // Arrange - 使用有效的邀请码字符（排除0, O, 1, I, L）
        String text = "快来加入测试团队【ABCD23】，一起协作处理图片水印吧！";

        // Act
        String extracted = InviteCodeUtils.extractCodeFromText(text);

        // Assert
        assertEquals("ABCD23", extracted);
    }

    @Test
    void extractCodeFromText_ShouldReturnNullWhenNoCode() {
        // Arrange
        String text = "快来加入测试团队，一起协作处理图片水印吧！";

        // Act
        String extracted = InviteCodeUtils.extractCodeFromText(text);

        // Assert
        assertNull(extracted);
    }

    @Test
    void extractCodeFromText_ShouldReturnNullForEmptyText() {
        // Act
        String extracted = InviteCodeUtils.extractCodeFromText("");

        // Assert
        assertNull(extracted);
    }

    @Test
    void extractCodeFromText_ShouldReturnNullForNullText() {
        // Act
        String extracted = InviteCodeUtils.extractCodeFromText(null);

        // Assert
        assertNull(extracted);
    }

    @Test
    void isValidCodeFormat_ShouldReturnTrueForValidCode() {
        // Arrange - 使用有效的邀请码字符（排除0, O, 1, I, L）
        String validCode = "ABCD23";

        // Act & Assert
        assertTrue(InviteCodeUtils.isValidCodeFormat(validCode));
    }

    @Test
    void isValidCodeFormat_ShouldReturnFalseForInvalidLength() {
        // Arrange
        String shortCode = "ABC";
        String longCode = "ABCD123";

        // Act & Assert
        assertFalse(InviteCodeUtils.isValidCodeFormat(shortCode));
        assertFalse(InviteCodeUtils.isValidCodeFormat(longCode));
    }

    @Test
    void isValidCodeFormat_ShouldReturnFalseForInvalidChars() {
        // Arrange - 包含易混淆字符 0, O, 1, I, L
        String codeWithZero = "ABCD0A";
        String codeWithO = "ABCDOA";
        String codeWithOne = "ABCD1A";
        String codeWithI = "ABCDIA";
        String codeWithL = "ABCDLA";

        // Act & Assert
        assertFalse(InviteCodeUtils.isValidCodeFormat(codeWithZero));
        assertFalse(InviteCodeUtils.isValidCodeFormat(codeWithO));
        assertFalse(InviteCodeUtils.isValidCodeFormat(codeWithOne));
        assertFalse(InviteCodeUtils.isValidCodeFormat(codeWithI));
        assertFalse(InviteCodeUtils.isValidCodeFormat(codeWithL));
    }

    @Test
    void isValidCodeFormat_ShouldReturnFalseForNull() {
        // Act & Assert
        assertFalse(InviteCodeUtils.isValidCodeFormat(null));
    }
}
