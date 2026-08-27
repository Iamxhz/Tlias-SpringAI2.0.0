package com.xhz.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类 — Phase 2 增强版
 *
 * <p>从 application.yml 读取密钥与过期时间，支持 Access Token + Refresh Token 双令牌。
 */
@Component
public class JwtUtils {

    // ==================== 静态字段（由 @Value 注入） ====================

    private static String SIGN_KEY;
    private static long ACCESS_EXPIRE;
    private static long REFRESH_EXPIRE;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @PostConstruct
    public void init() {
        JwtUtils.SIGN_KEY = this.secret;
        JwtUtils.ACCESS_EXPIRE = this.accessExpiration;
        JwtUtils.REFRESH_EXPIRE = this.refreshExpiration;
    }

    // ==================== Token 生成 ====================

    /**
     * 生成 Access Token（12h），包含用户标识 + 角色 + 权限。
     */
    public static String generateAccessToken(Map<String, Object> claims) {
        return Jwts.builder()
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256, SIGN_KEY)
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRE))
                .compact();
    }

    /**
     * 生成 Refresh Token（7d），仅包含用户标识（用于续期 Access Token）。
     */
    public static String generateRefreshToken(Integer userId, String username) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("username", username)
                .signWith(SignatureAlgorithm.HS256, SIGN_KEY)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRE))
                .compact();
    }

    /**
     * 向后兼容旧代码：生成 JWT（等同于 Access Token）。
     *
     * @deprecated 新代码请使用 {@link #generateAccessToken(Map)}
     */
    @Deprecated
    public static String generateJwt(Map<String, Object> claims) {
        return generateAccessToken(claims);
    }

    // ==================== Token 解析 ====================

    /**
     * 解析 JWT 令牌
     *
     * @param jwt JWT 令牌字符串
     * @return JWT Claims（包含载荷中的所有键值对）
     */
    public static Claims parseJWT(String jwt) {
        return Jwts.parser()
                .setSigningKey(SIGN_KEY)
                .parseClaimsJws(jwt)
                .getBody();
    }
}
