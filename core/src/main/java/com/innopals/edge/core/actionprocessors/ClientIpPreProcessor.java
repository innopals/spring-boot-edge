package com.innopals.edge.core.actionprocessors;

import com.innopals.edge.EdgeActionPreProcessor;
import com.innopals.edge.EdgeContext;
import com.innopals.edge.annotations.EnableEdge;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;

/**
 * The opinionated authentication and authorization process for edge configured by edge annotations.
 *
 * @author bestmike007
 */
@Slf4j
public class ClientIpPreProcessor implements EdgeActionPreProcessor {

  private static final String IPV4_DOT = ".";
  private static final String IPV4_PATTERN = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
  private static final String CIDR_PATTERN = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\/([012]?\\d|3[012])$";
  private static final String UNKNOWN_IP = "unknown";

  private static int ip2int(String ip) {
    // suppose ip is already a valid ipv4 string
    int rs = 0;
    for (String n : ip.split(IPV4_DOT)) {
      rs = rs << 8 + Integer.parseInt(n);
    }
    return rs;
  }

  private static Function<String, Boolean> createCidrMatcher(String cidr) {
    // suppose cidr is valid.
    String[] n = cidr.split("/");
    int mask = ~((1 << Integer.parseInt(n[1])) - 1);
    int ipPrefix = ip2int(n[0]) & mask;
    return ip -> ip.matches(IPV4_PATTERN) && (ip2int(ip) & mask) == ipPrefix;
  }

  private final Function<String, Boolean> isTrustedProxy;

  public ClientIpPreProcessor(EnableEdge applicationConfig) {
    List<Function<String, Boolean>> matchers = new LinkedList<>();
    matchers.add(ip -> StringUtils.equals(ip, "127.0.0.1"));
    String[] trustedProxies = applicationConfig.trustedProxies();
    Arrays.stream(trustedProxies).forEach(s -> {
      if (s.matches(IPV4_PATTERN)) {
        matchers.add(ip -> StringUtils.equals(ip, s));
      } else if (s.matches(CIDR_PATTERN)) {
        matchers.add(createCidrMatcher(s));
      } else {
        log.warn("Unknown trusted proxy config: {}, ignored.", s);
      }
    });
    this.isTrustedProxy = ip -> matchers.stream().anyMatch(matcher -> matcher.apply(ip));
  }

  /**
   * 获取真实IP
   *
   * @param request HttpServletRequest
   * @return ip
   */
  private String[] getForwardedIpList(HttpServletRequest request) {
    String forwardedIp = request.getHeader("X-Forwarded-For");
    //检查X-Forwarded-For是否为空。
    if (StringUtils.isEmpty(forwardedIp) || UNKNOWN_IP.equalsIgnoreCase(forwardedIp)) {
      forwardedIp = request.getHeader("Proxy-Client-IP");
    }
    if (StringUtils.isEmpty(forwardedIp) || UNKNOWN_IP.equalsIgnoreCase(forwardedIp)) {
      forwardedIp = request.getHeader("WL-Proxy-Client-IP");
    }
    if (StringUtils.isEmpty(forwardedIp) || UNKNOWN_IP.equalsIgnoreCase(forwardedIp)) {
      forwardedIp = "";
    }
    Stack<String> ipList = new Stack<>();
    Arrays.stream(forwardedIp.split(",")).forEach(ip -> ipList.push(StringUtils.trim(ip)));
    ipList.push(request.getRemoteAddr());
    Stack<String> allowedIpList = new Stack<>();
    while (ipList.size() > 0) {
      val ip = ipList.pop();
      allowedIpList.push(ip);
      if (!ip.matches(IPV4_PATTERN) || !isTrustedProxy.apply(ip)) {
        break;
      }
    }
    String[] rs = new String[allowedIpList.size()];
    for (int i = 0; i < rs.length; i++) {
      rs[i] = allowedIpList.pop();
    }
    return rs;
  }

  @Override
  public void beforeAction(EdgeContext context) {
    String[] forwardedIps = getForwardedIpList(context.getRequest());
    String realIp = forwardedIps[0];
    context.setForwardedIps(forwardedIps);
    context.setRealIp(realIp);
  }
}
