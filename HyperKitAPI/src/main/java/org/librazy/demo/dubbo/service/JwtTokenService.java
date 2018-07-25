package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.UserEntity;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * JWT Token 服务
 */
public interface JwtTokenService {

    /**
     * 获取过期时间
     *
     * @return 过期时间
     */
    long getExpiration();

    /**
     * 设置过期时间
     *
     * @param expiration 过期时间
     */
    void setExpiration(long expiration);

    /**
     * 获取最大刷新次数
     *
     * @return 最大刷新次数
     */
    long getMaximumRefresh();

    /**
     * 设置最大刷新次数
     *
     * @param maximumRefresh 最大刷新次数
     */
    void setMaximumRefresh(long maximumRefresh);

    /**
     * 检查 Token 有效性
     *
     * @param token Token
     * @return Token 中的声明
     */
    Map<String, Object> validateClaimsFromToken(String token);

    /**
     * 生成 JWT Token
     *
     * @param user    用户
     * @param session 会话
     * @return Token
     */
    String generateToken(UserEntity user, String session);

    /**
     * 刷新 Token
     *
     * @param token 原 Token
     * @return 新 Token 或 Null
     */
    @Nullable
    String refreshToken(String token);

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳
     */
    long getClock();

    /**
     * 设置时间戳（Null 清除时间戳）
     *
     * @param clock 时间戳
     */
    void setClock(@Nullable Long clock);
}
