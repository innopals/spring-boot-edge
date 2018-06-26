package com.innopals.edge.annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;

import java.lang.annotation.*;

/**
 * @author bestmike007
 * Edge config for controllers.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
public @interface EdgeController {
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
   * Specify the default internal auth type for current controller to send request to backend service.
   */
  BackendAuthType backendAuthType() default BackendAuthType.USE_PARENT;

  /***
   * Default auth secret for internal request for current controller, if internalAuthType is {@link BackendAuthType}.JWT_HMAC256
   */
  String backendAuthSecret() default "";

  /***
   * Set controller default auth level, will be override by action config.
   */
  AuthLevel authLevel() default AuthLevel.USE_PARENT;

  /***
   * Default authorization expression, can be override by action config.
   */
  String authExpression() default "";
}
