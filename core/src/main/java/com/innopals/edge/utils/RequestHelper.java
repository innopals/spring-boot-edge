package com.innopals.edge.utils;


import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * @author bestmike007
 * Request Utils
 */
public class RequestHelper {

  private static final Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
  private static final String UNKNOWN_IP = "unknown";

  /**
   * 获取真实IP
   *
   * @param request HttpServletRequest
   * @return ip
   */
  public static String getRealIp(HttpServletRequest request) {
    String forwardedIp = request.getHeader("X-Forwarded-For");
    String clientRealIp;
    //检查X-Forwarded-For是否为空。
    if (forwardedIp == null || forwardedIp.length() == 0 || UNKNOWN_IP.equalsIgnoreCase(forwardedIp)) {
      forwardedIp = request.getHeader("Proxy-Client-IP");
    }
    if (forwardedIp == null || forwardedIp.length() == 0 || UNKNOWN_IP.equalsIgnoreCase(forwardedIp)) {
      forwardedIp = request.getHeader("WL-Proxy-Client-IP");
    }
    if (forwardedIp == null || forwardedIp.length() == 0 || UNKNOWN_IP.equalsIgnoreCase(forwardedIp)) {
      clientRealIp = request.getRemoteAddr();
    } else {
      clientRealIp = forwardedIp.split(",")[0];
      if (PATTERN.matcher(clientRealIp).matches()) {
        return "0:0:0:0:0:0:0:1".equals(clientRealIp) ? "127.0.0.1" : clientRealIp;
      }
    }
    return "0:0:0:0:0:0:0:1".equals(clientRealIp) ? "127.0.0.1" : clientRealIp;
  }
}
