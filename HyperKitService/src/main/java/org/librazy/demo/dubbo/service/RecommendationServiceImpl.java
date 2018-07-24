package org.librazy.demo.dubbo.service;


import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.model.RecommendBlogEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Service;

@Service
@Component
public class RecommendationServiceImpl implements RecommendationService {

	//private ElasticSearchServiceImpl elasticsearchService; 
	private BlogRepository blogRepository;
	//private RestHighLevelClient client;
	private RestHighLevelClient client;

    @Autowired
    public RecommendationServiceImpl(RestHighLevelClient client) {
        this.client = client;
    }
	
	
	@Override
	public BigInteger hash(String source) {
		if (source == null || source.length() == 0) {
			return new BigInteger("0");
		} else {
			char[] sourceArray = source.toCharArray();
			BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
			BigInteger m = new BigInteger("1000003");
			BigInteger mask = new BigInteger("2").pow(64).subtract(
					new BigInteger("1"));
			for (char item : sourceArray) {
				BigInteger temp = BigInteger.valueOf((long) item);
				x = x.multiply(m).xor(temp).and(mask);
			}
			x = x.xor(new BigInteger(String.valueOf(source.length())));
			if (x.equals(new BigInteger("-1"))) {
				x = new BigInteger("-2");
			}
			return x;
		}
	}


	@Override
	public BigInteger simhash(List<String> ik) {
		int[] v = new int[64];
		for(String word:ik)
		{
			BigInteger t=this.hash(word);
			for (int i = 0; i < 64; i++) 
			{
				BigInteger bitmask = new BigInteger("1").shiftLeft(i);
				if (t.and(bitmask).signum() != 0) {
					// 这里是计算整个文档的所有特征的向量和
					// 这里实际使用中需要 +- 权重，比如词频，而不是简单的 +1/-1，
					v[i] += 1;
				} else {
					v[i] -= 1;
				}
			}
		}
		
		BigInteger simhashfinger = new BigInteger("0");
		for (int i = 0; i < 64; i++) {
			if (v[i] >= 0) {
				simhashfinger = simhashfinger.add(new BigInteger("1").shiftLeft(i));
			}
		}
		return simhashfinger;
	}

	@Override
	public BigInteger getSimhash(long id) {
		// TODO Auto-generated method stub
		return this.blogRepository.getOne(id).getSimhash();
	}


	@Override
	public List<RecommendBlogEntry> recommendation(long id) {
		
		List<RecommendBlogEntry> results =new LinkedList<RecommendBlogEntry>();
		
		BigInteger thisSimhash=this.getSimhash(id);
		
		
		
		
		
		
		
		return results;
	}


	@Override
	public List<String> ikAnalyze(String content ) {
		
		
		
		
		return null;
	}

}
