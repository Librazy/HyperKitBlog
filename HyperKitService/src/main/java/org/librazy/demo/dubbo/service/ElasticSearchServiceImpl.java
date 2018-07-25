package org.librazy.demo.dubbo.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.BlogEntrySearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Component
public class ElasticSearchServiceImpl implements ElasticSearchService {

    private static Logger logger = LoggerFactory.getLogger(SrpSessionServiceImpl.class);

    @Value("${es.blog.index}")
    private String index;

    @Value("${es.blog.type}")
    private String type;

    private static final String TITLE = "title";

    private static final String CONTENT = "content";

    private final RestHighLevelClient client;

    private final ObjectMapper mapper;

    @Autowired
    public ElasticSearchServiceImpl(RestHighLevelClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public void put(BlogEntry entry) throws IOException {
        String json = mapper.writeValueAsString(entry);
        String id = String.valueOf(entry.getId());
        IndexRequest indexRequest = new IndexRequest(index, type, id).source(json, XContentType.JSON);
        logger.info("EsIndexing {} {} {}", index, type, id);
        logger.debug("EsSource {}", json);
        IndexResponse indexResponse = client.index(indexRequest);
        logger.info("EsIndexing request {} successful with {}", indexRequest, indexResponse);
    }

    @Override
    public void delete(long id) throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest(index, type, String.valueOf(id));
        logger.info("EsDeleting {} {} {}", index, type, id);
        DeleteResponse deleteResponse = client.delete(deleteRequest);
        logger.info("EsDeleting request {} successful with {}", deleteRequest, deleteResponse);

    }

    @Override
    public void update(BlogEntry entry)throws IOException{
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("authorId", entry.getAuthorId());
        jsonMap.put("title", entry.getTitle());
        jsonMap.put("publish", entry.getPublish());
        jsonMap.put("updated", entry.getUpdated());
        jsonMap.put("content", entry.getContent());
        jsonMap.put("hash", "0");
        UpdateRequest request = new UpdateRequest(
                index,
                type,
                entry.getId().toString());
        request.doc(jsonMap);
        UpdateResponse updateResponse = client.update(request);
        logger.info("EsDeleting request {} successful with {}", request, updateResponse);
    }

    @Override
    public List<BlogEntrySearchResult> search(String text) throws IOException {
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.types(type);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.multiMatchQuery(text, TITLE, CONTENT));
        sourceBuilder.from(0);
        sourceBuilder.size(10);
        sourceBuilder.fetchSource(TITLE, CONTENT);
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(CONTENT).fragmenter("simple");
        highlightBuilder.field(TITLE, Integer.MAX_VALUE, 1);
        sourceBuilder.highlighter(highlightBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        logger.info("EsSearch request {} successful with {}", searchRequest, searchResponse);
        List<BlogEntrySearchResult> results = new ArrayList<>((int) searchResponse.getHits().getTotalHits());
        for (SearchHit hit : searchResponse.getHits()) {
            long id = Long.parseLong(hit.getId());
            HighlightField titleHighlight = hit.getHighlightFields().get(TITLE);
            String title = titleHighlight != null ? titleHighlight.getFragments()[0].string() : (String) hit.getSourceAsMap().get(TITLE);
            String content = Arrays.stream(hit.getHighlightFields().get(CONTENT).getFragments()).map(Text::string).collect(Collectors.joining("\", \"", "\"", "\""));
            BlogEntrySearchResult result = new BlogEntrySearchResult(id, title, content);
            results.add(result);
        }
        return results;
    }
}
