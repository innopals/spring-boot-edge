package com.innopals.edge;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bestmike007
 */
public interface SessionIdResolver {
  /**
   * @param request the http servlet request
   * @return current session id or null if not exists
   */
  String resolveSessionId(HttpServletRequest request);
}
