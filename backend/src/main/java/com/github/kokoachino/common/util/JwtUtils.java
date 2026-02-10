package com.github.kokoachino.common.util;

import com.github.kokoachino.common.enums.TokenTypeEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * JWT 工具类
 * 支持单 Token 自动续期机制
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.auto-renew-threshold}")
    private Long autoRenewThreshold;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token
     */
    public String generateToken(Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TokenTypeEnum.ACCESS.getValue());
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Integer getUserIdFromToken(String token) {
        return Integer.parseInt(getClaimsFromToken(token).getSubject());
    }

    /**
     * 从 Token 中获取 Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 有效性
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     */
    private boolean isTokenExpired(String token) {
        Date expirationDate = getClaimsFromToken(token).getExpiration();
        return expirationDate.before(new Date());
    }

    /**
     * 检查 Token 是否需要续期
     * 当剩余有效期小于续期阈值时返回 true
     */
    public boolean shouldRenewToken(String token) {
        try {
            Date expirationDate = getClaimsFromToken(token).getExpiration();
            long remainingTime = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
            return remainingTime > 0 && remainingTime < autoRenewThreshold;
        } catch (Exception e) {
            return false;
        }
    }
}