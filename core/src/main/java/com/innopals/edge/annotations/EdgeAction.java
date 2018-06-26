package com.innopals.edge.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author bestmike007
 * Edge config for actions.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EdgeAction {
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
   * Specify the internal auth type for current action to send request to backend service.
   */
  BackendAuthType backendAuthType() default BackendAuthType.USE_PARENT;

  /***
   * Specify the auth secret for internal request specified for current action,
   * if internalAuthType is {@link BackendAuthType}.JWT_HMAC256
   */
  String backendAuthSecret() default "";

  /***
   * Backend path for this action, path parameter supported, e.g. "/user/${userId}"
   */
  String backendPath() default "";

  /***
   * Http method to send request to backend, "GET", "POST", "PUT", "DELETE",
   * or empty string for auto detection from spring mvc mappings.
   */
  String backendHttpMethod() default "";

  /***
   * Action auth level
   */
  AuthLevel authLevel() default AuthLevel.USE_PARENT;

  /***
   * Authorization expression
   */
  String authExpression() default "";
}
