package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.model.RecommendBlogEntry;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public interface RecommendationService {

    List<String> ikAnalyze(String content) throws IOException;

    BigInteger hash(String source);

    String simhash(List<String> ik);

    default String simhash(String content) throws IOException {
        return simhash(ikAnalyze(content));
    }

    List<RecommendBlogEntry> recommend(String simhash) throws IOException;
}
