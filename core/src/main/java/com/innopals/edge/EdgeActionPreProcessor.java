package com.innopals.edge;

/**
 * @author bestmike007
 */
public interface EdgeActionPreProcessor {
  /**
   * Pre-processor for action to do authentication, authorization or other assertions.
   * @param context the edge context
   * @throws Exception any exception to interrupt the process.
   */
  void beforeAction(EdgeContext context) throws Exception;
}
