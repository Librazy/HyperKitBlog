package org.librazy.demo.dubbo.service;


import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.model.BlogEntrySearchResult;
import org.librazy.demo.dubbo.model.RecommendBlogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Service;

import net.minidev.json.JSONArray;

@Service
@Component
public class RecommendationServiceImpl implements RecommendationService {

	//private ElasticSearchServiceImpl elasticsearchService; 
	private static Logger logger = LoggerFactory.getLogger(SrpSessionServiceImpl.class);

    @Value("${es.blog.index}")
    private String index;

    @Value("${es.blog.type}")
    private String type;
    
    private static final String TITLE = "title";


	
	
	private BlogRepository blogRepository;
	//private RestHighLevelClient client;
	private TransportClient  client;

    @Autowired
    public RecommendationServiceImpl(TransportClient  client,BlogRepository blogRepository) {
        this.client = client;
        this.blogRepository=blogRepository;
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
	public String simhash(List<String> ik) {
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
		
		StringBuffer simhashfinger = new StringBuffer();
		for (int i = 0; i < 64; i++) {
			if (v[i] > 0) {
				v[i]=1;
			}
			else
			{
				v[i]=0;
			}
			simhashfinger = simhashfinger.append(v[i]);
		}
		return simhashfinger.toString();
	}



	@Override
	public List<RecommendBlogEntry> recommendation(long id) {
		
		List<RecommendBlogEntry> results =new LinkedList<RecommendBlogEntry>();
		
		String thisSimhash=this.blogRepository.getOne(id).getSimhash();
		
		SearchRequestBuilder responsebuilder = client.prepareSearch(this.index).setTypes(this.type);
		SearchResponse searchResponse=responsebuilder.setQuery(
				QueryBuilders.fuzzyQuery("simhash", thisSimhash).fuzziness(Fuzziness.fromEdits(2)).prefixLength(2).maxExpansions(50)
				) .get();
		
		for (SearchHit hit : searchResponse.getHits()) {
            long id1 = Long.parseLong(hit.getId());
            HighlightField titleHighlight = hit.getHighlightFields().get(TITLE);
            String title = titleHighlight != null ? titleHighlight.getFragments()[0].string() : (String) hit.getSourceAsMap().get(TITLE);
            //String content = Arrays.stream(hit.getHighlightFields().get(CONTENT).getFragments()).map(Text::string).collect(Collectors.joining("\", \"", "\"", "\""));
            RecommendBlogEntry result = new RecommendBlogEntry(id1, title);
            results.add(result);
        }
		return results;
	}


	@Override
	public List<String> ikAnalyze(String content ) {
		
		//TransportClient client = EsUtils.getSingleClient();
		
		List<String> results=new ArrayList<String>();
		
		AnalyzeRequest analyzeRequest = new AnalyzeRequest(this.index)
                .text(content)
                .analyzer("ik_max_word");
		
		List<AnalyzeResponse.AnalyzeToken> tokens = this.client.admin().indices()
                .analyze(analyzeRequest)
                .actionGet()
                .getTokens();
		
		for (AnalyzeResponse.AnalyzeToken token : tokens) {
			results.add(token.getTerm());
        }
		return results;
	}
}





