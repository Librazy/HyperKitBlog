package org.librazy.demo.dubbo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import java.io.IOException;

/**
 * SRP Session 服务
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public interface SrpSessionService {

    /**
     * 新建 SRP 会话
     *
     * @param n SRP 参数 N
     * @param g SRP 参数 G
     */
    void newSession(String n, String g);

    /**
     * 还原 SRP 会话
     *
     * @param id 会话 ID
     * @throws IOException            反序列化失败
     * @throws ClassNotFoundException 反序列化失败
     */
    void loadSession(long id) throws IOException, ClassNotFoundException;

    /**
     * SRP 协商第一步
     *
     * @param account SRP 账号
     * @return SRP 参数 B
     * @throws IOException 序列化失败
     */
    String step1(SrpAccountEntity account) throws IOException;

    /**
     * SRP 协商第二步
     *
     * @param a  SRP 参数A
     * @param m1 SRP 参数M1
     * @param ua 用户代理
     * @return SRP 参数 M2
     */
    String step2(String a, String m1, String ua);

    /**
     * SRP 会话密钥
     *
     * @param doHash 是否哈希
     * @return SRP 会话密钥
     */
    String getSessionKey(boolean doHash);

    /**
     * 保存注册表单
     *
     * @param signupForm 注册表单
     * @throws JsonProcessingException JSON 序列化失败
     */
    void saveSignup(SrpSignupForm signupForm) throws JsonProcessingException;

    /**
     * 获取注册表单
     *
     * @return 注册表单
     * @throws IOException JSON 反序列化失败
     */
    SrpSignupForm getSignup() throws IOException;

    /**
     * 完成注册，写入数据库
     *
     * @param now 新 ID
     */
    void confirmSignup(long now);

    /**
     * 清除会话状态
     */
    void clear();
}
