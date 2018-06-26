package com.innopals.edge.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innopals.edge.EdgeActionProcessor;
import com.innopals.edge.EdgeContext;
import com.innopals.edge.core.actionprocessors.MapToBackendActionProcessor;
import okhttp3.OkHttpClient;

import java.util.LinkedList;
import java.util.List;

/**
 * @author bestmike007
 */
public class DefaultEdgeActionProcessor implements EdgeActionProcessor {

  private final List<EdgeActionProcessor> processors;

  DefaultEdgeActionProcessor(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
    processors = new LinkedList<>();
    processors.add(new MapToBackendActionProcessor(objectMapper, okHttpClient));
  }

  @Override
  public Object process(EdgeContext context, Object prevResult) throws Exception {
    Object result = prevResult;
    for (EdgeActionProcessor processor : processors) {
      result = processor.process(context, result);
    }
    return result;
  }
}