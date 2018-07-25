package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.model.BlogEntry;
import org.librazy.demo.dubbo.model.BlogEntrySearchResult;

import java.io.IOException;
import java.util.List;

public interface ElasticSearchService {

    void put(BlogEntry entry) throws IOException;

    void delete(long id) throws IOException;

    List<BlogEntrySearchResult> search(String text) throws IOException;
}
