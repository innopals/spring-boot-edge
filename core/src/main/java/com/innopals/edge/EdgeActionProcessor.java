package com.innopals.edge;

/**
 * @author bestmike007
 */
public interface EdgeActionProcessor {
  /**
   * Process an edge action according to the context
   * @param context the context for current action
   * @param prevResult result from the previous processor
   * @throws Exception any exception
   * @return result for spring mvc or null if intercepted
   */
  Object process(EdgeContext context, Object prevResult) throws Exception;
}
