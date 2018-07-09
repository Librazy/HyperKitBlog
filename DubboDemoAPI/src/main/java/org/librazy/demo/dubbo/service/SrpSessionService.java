package org.librazy.demo.dubbo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;

import java.io.IOException;

public interface SrpSessionService {
    void newSession(String n, String g);

    void loadSession(long id) throws IOException, ClassNotFoundException;

    String step1(SrpAccountEntity account) throws IOException;

    String step2(String a, String m1, String ua);

    String getSessionKey(boolean doHash);

    void saveSignup(SrpSignupForm signupForm) throws JsonProcessingException;

    SrpSignupForm getSignup() throws IOException;

    void confirmSignup(long now);
}
