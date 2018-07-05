package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;

public interface SrpSessionService {
    void newSession(String N, String g);

    void loadSession(long id);

    String step1(SrpAccountEntity account);

    String step2(String A, String M1, String ua) throws Exception;

    String getSessionKey(boolean doHash);

    void saveSignup(SrpSignupForm signupForm);

    SrpSignupForm getSignup();

    void confirmSignup(long now);
}
