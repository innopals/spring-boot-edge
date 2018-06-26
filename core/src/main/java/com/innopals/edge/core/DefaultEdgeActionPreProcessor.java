package com.innopals.edge.core;

import com.innopals.edge.*;
import com.innopals.edge.annotations.EnableEdge;
import com.innopals.edge.core.actionprocessors.AuthPreProcessor;
import com.innopals.edge.core.actionprocessors.ClientIpPreProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * The opinionated authentication and authorization process for edge configured by edge annotations.
 *
 * @author bestmike007
 */
@Slf4j
public class DefaultEdgeActionPreProcessor implements EdgeActionPreProcessor {

  private final EnableEdge applicationConfig;
  private final List<EdgeActionPreProcessor> preProcessors = new LinkedList<>();

  public DefaultEdgeActionPreProcessor(EnableEdge applicationConfig) {
    this.applicationConfig = applicationConfig;
    preProcessors.add(new ClientIpPreProcessor(applicationConfig));
    preProcessors.add(new AuthPreProcessor());
  }

  @Override
  public void beforeAction(EdgeContext context) throws Exception {
    for(EdgeActionPreProcessor preProcessor: preProcessors) {
      preProcessor.beforeAction(context);
    }
  }
}
