package org.onebusaway.nyc.presentation.impl.sort;

import org.onebusaway.nyc.presentation.service.search.SearchResult;

import java.util.Comparator;

public class SearchResultComparator implements Comparator<SearchResult> {

  @Override
  public int compare(SearchResult o1, SearchResult o2) {
    return o1.getName().compareTo(o2.getName());
  }
}