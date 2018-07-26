package org.librazy.demo.dubbo.service;


import com.alibaba.dubbo.config.annotation.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.librazy.demo.dubbo.model.RecommendBlogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Component
public class RecommendationServiceImpl implements RecommendationService {

    private static final String TITLE = "title";
    private static final String CONTENT = "content";
    private static Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    @Value("${es.blog.index}")
    private String index;
    @Value("${es.blog.type}")
    private String type;
    private RestHighLevelClient client;

    private ObjectMapper objectMapper;

    @Autowired
    public RecommendationServiceImpl(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }


    @Override
    public BigInteger hash(String source) {
        if (source == null || source.length() == 0) {
            return BigInteger.valueOf(0);
        } else {
            char[] sourceArray = source.toCharArray();
            BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
            BigInteger m = BigInteger.valueOf(1000003);
            BigInteger mask = BigInteger.valueOf(2).pow(64).subtract(
                    BigInteger.valueOf(1));
            for (char item : sourceArray) {
                BigInteger temp = BigInteger.valueOf((long) item);
                x = x.multiply(m).xor(temp).and(mask);
            }
            x = x.xor(new BigInteger(String.valueOf(source.length())));
            if (x.equals(BigInteger.valueOf(-1))) {
                x = BigInteger.valueOf(-2);
            }
            return x;
        }
    }


    @Override
    public String simhash(List<String> ik) {
        int[] v = new int[64];
        for (String word : ik) {
            BigInteger t = this.hash(word);
            for (int i = 0; i < 64; i++) {
                BigInteger bitmask = BigInteger.valueOf(1).shiftLeft(i);
                if (t.and(bitmask).signum() != 0) {
                    v[i] += 1;
                } else {
                    v[i] -= 1;
                }
            }
        }

        StringBuilder simhashfinger = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            if (v[i] > 0) {
                v[i] = 1;
            } else {
                v[i] = 0;
            }
            simhashfinger.append(v[i]);
        }
        return simhashfinger.toString();
    }


    @Override
    public List<RecommendBlogEntry> recommend(String simhash) throws IOException {
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.types(type);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.fuzzyQuery("simhash", simhash).fuzziness(Fuzziness.TWO).maxExpansions(200).transpositions(true));
        sourceBuilder.from(0);
        sourceBuilder.size(10);
        sourceBuilder.fetchSource(TITLE, CONTENT);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        logger.info("EsSearch request {} successful with {}", searchRequest, searchResponse);
        List<RecommendBlogEntry> results = new ArrayList<>((int) searchResponse.getHits().getTotalHits());
        for (SearchHit hit : searchResponse.getHits()) {
            long id = Long.parseLong(hit.getId());
            String title = String.valueOf(Objects.requireNonNull(hit.getSourceAsMap().get(TITLE)));
            RecommendBlogEntry result = new RecommendBlogEntry(id, title);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<String> ikAnalyze(String content) throws IOException {
        String jsonString =
                "{\"tokenizer\" : \"ik_smart\",\n" +
                        "\"char_filter\" : [\"html_strip\"]," +
                        "\"text\":\"" + content.replaceAll("\"", "\\\\\"") + "\"}";
        NStringEntity entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON);
        Response response = client.getLowLevelClient().performRequest("GET", "/_analyze", Collections.emptyMap(), entity);
        TypeFactory factory = TypeFactory.defaultInstance();
        MapType t = factory.constructMapType(HashMap.class, String.class, String.class);
        CollectionType t2 = factory.constructCollectionType(ArrayList.class, t);
        MapType t3 = factory.constructMapType(HashMap.class, factory.constructType(String.class), t2);
        HashMap<String, List<HashMap<String, String>>> result = objectMapper.readValue(response.getEntity().getContent(), t3);//
        return result.get("tokens").stream().map(tokenEntry -> tokenEntry.get("token")).collect(Collectors.toList());
    }
}
