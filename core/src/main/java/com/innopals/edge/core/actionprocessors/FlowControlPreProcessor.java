package com.innopals.edge.core.actionprocessors;

import com.innopals.edge.EdgeActionPreProcessor;
import com.innopals.edge.EdgeContext;
import lombok.extern.slf4j.Slf4j;

/**
 * The opinionated authentication and authorization process for edge configured by edge annotations.
 *
 * @author bestmike007
 */
@Slf4j
public class FlowControlPreProcessor implements EdgeActionPreProcessor {

  @Override
  public void beforeAction(EdgeContext context) throws Exception {
    // TODO apply rate limit for each action
  }
}
