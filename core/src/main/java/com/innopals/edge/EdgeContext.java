package com.innopals.edge;

import com.fasterxml.jackson.databind.JavaType;
import com.innopals.edge.core.EdgeActionConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author bestmike007
 */
public final class EdgeContext {

  private HttpServletRequest request;
  private HttpServletResponse response;
  private UserIdentity userIdentity;
  private final SessionIdResolver sessionIdResolver;
  private final SessionStore sessionStore;
  @Getter
  private final EdgeActionConfig actionConfig;
  @Getter
  private final List<Consumer<HttpServletResponse>> proxyResponseHeaderProcessors;
  @Getter
  private JavaType backendResultType;
  @Getter
  private Function postProcessor;
  @Getter
  @Setter
  private Object[] actionArgs;
  @Getter
  @Setter
  private String[] forwardedIps;
  @Getter
  @Setter
  private String realIp;

  public EdgeContext(@NotNull SessionStore sessionStore, @NotNull SessionIdResolver sessionIdResolver, @NotNull EdgeActionConfig actionConfig) {
    this.sessionStore = sessionStore;
    this.sessionIdResolver = sessionIdResolver;
    this.actionConfig = actionConfig;
    this.proxyResponseHeaderProcessors = new LinkedList<>();
  }

  public HttpServletRequest getRequest() {
    if (request != null) {
      return request;
    }
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    return request = attributes.getRequest();
  }

  public HttpServletResponse getResponse() {
    if (response != null) {
      return response;
    }
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    return response = attributes.getResponse();
  }

  public @NotNull UserIdentity currentUser() {
    if (userIdentity != null) {
      return userIdentity;
    }
    String sessionId = sessionIdResolver.resolveSessionId(getRequest());
    if (StringUtils.isNotEmpty(sessionId)) {
      userIdentity = sessionStore.getUserIdentity(sessionId);
    }
    if (userIdentity == null) {
      userIdentity = new UserIdentity();
    }
    return userIdentity;
  }

  /***
   * Set post processor.
   * @param handler Use the JavaType to parse backend result and map it to data type U
   * @param <O> backend result data type
   * @param <U> result data type
   * @return the result converted from backend
   */
  public <O, U> EdgeContext postProcess(JavaType backendResultType, Function<Supplier<O>, U> handler) {
    this.backendResultType = backendResultType;
    postProcessor = handler;
    return null;
  }

  /***
   * Let the edge context proceed with backend mapping
   * @param <T> the result data type
   * @return always null to bypass spring mvc handlers
   */
  public @Null <T> T proceed() {
    return null;
  }

  public void setHeader(String name, String value) {
    proxyResponseHeaderProcessors.add(response -> response.setHeader(name, value));
  }
}
