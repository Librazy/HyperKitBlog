package org.librazy.demo.dubbo.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;


import org.librazy.demo.dubbo.model.RecommendBlogEntry;

public interface RecommendationService {

    List<String> ikAnalyze(String content);
	
	BigInteger hash(String source);
	
	String simhash(List<String> ik);

	default String simhash(String content) {
	    return simhash(ikAnalyze(content));
    }

    List<RecommendBlogEntry> recommend(String simhash) throws IOException;
}
