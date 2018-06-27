package com.innopals.edge.annotations;

import com.innopals.edge.config.EdgeConfiguration;
import com.innopals.edge.core.EdgeControllerAdvice;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author bestmike007
 * Enable opionated api edge gateway based on pre-configured spring boot.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableAutoConfiguration
@Import({
  EdgeConfiguration.class,
  EdgeControllerAdvice.class,
})
public @interface EnableEdge {

  /***
   * Alias for baseUrl, if baseUrl is specified, this value will be ignored.
   */
  @AliasFor("baseUrl")
  String value() default "";

  /***
   * Default backend base url for this edge gateway.
   */
  @AliasFor("value")
  String baseUrl() default "";

  /***
   * Specify the default internal backend auth type to send request to backend service.
   */
  BackendAuthType backendAuthType() default BackendAuthType.NONE;

  /***
   * Default auth secret for internal request, if internalAuthType is {@link BackendAuthType}.JWT_HMAC256
   */
  String backendAuthSecret() default "";

  /***
   * Specify the cookie key for session to enable authentication/authorization by cookie.
   */
  String cookieSessionIdKey() default "s";

  /***
   * Enable header authentication/authorization, e.g. "Authorization: Bearer AbCdEfG12346"
   */
  boolean headerAuthorization() default false;

  /***
   * Session id length, edge will generate the session id of this length, e.g. [0-9a-zA-Z]{16}
   */
  int sessionIdLength() default 16;

  /**
   * Trusted reverse proxy CIDR/IP list.
   */
  String[] trustedProxies() default {};

  /***
   * Set application default auth level, will be override by controller/action config.
   */
  AuthLevel authLevel() default AuthLevel.AUTHENTICATED;

  /***
   * Default authorization expression, can be override by controller/action config.
   */
  String authExpression() default "";

  /***
   * If set to true, edge will always wrap internal exception with a specific code defined below.
   */
  boolean wrapException() default true;

  String uncaughtExceptionCode() default "500";

  String illegalArgumentExceptionCode() default "400";

  String unauthenticatedExceptionCode() default "401";

  String unauthorizedExceptionCode() default "403";

  String tooManyRequestsExceptionCode() default "429";
}
