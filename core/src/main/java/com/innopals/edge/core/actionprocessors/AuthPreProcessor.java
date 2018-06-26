package com.innopals.edge.core.actionprocessors;

import com.innopals.edge.*;
import com.innopals.edge.annotations.AuthLevel;
import com.innopals.edge.core.EdgeActionConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The opinionated authentication and authorization process for edge configured by edge annotations.
 *
 * @author bestmike007
 */
@Slf4j
public class AuthPreProcessor implements EdgeActionPreProcessor {

  private static final String ROLE_PREFIX = "role:";

  private static final WeakHashMap<Method, Consumer<UserIdentity>> PROCESSOR_CACHE = new WeakHashMap<>();

  private static Consumer<UserIdentity> createProcessor(EdgeActionConfig actionConfig) {
    // Authentication
    AuthLevel authLevel = actionConfig.getActionConfig().authLevel();
    if (authLevel == AuthLevel.USE_PARENT) {
      authLevel = actionConfig.getControllerConfig().authLevel();
    }
    if (authLevel == AuthLevel.USE_PARENT) {
      authLevel = actionConfig.getApplicationConfig().authLevel();
    }
    if (authLevel == AuthLevel.USE_PARENT) {
      authLevel = AuthLevel.AUTHENTICATED;
    }
    if (authLevel == AuthLevel.UNAUTHENTICATED) {
      return user -> {};
    }
    Consumer<UserIdentity> processor = user -> {
      if (StringUtils.isEmpty(user.getId())) {
        throw new ActionUnauthenticatedException();
      }
    };
    if (authLevel != AuthLevel.REQUIRE_AUTHORIZATION) {
      return processor;
    }
    // Authorization
    String authExpression = actionConfig.getActionConfig().authExpression();
    if (StringUtils.isEmpty(authExpression)) {
      authExpression = actionConfig.getControllerConfig().authExpression();
    }
    if (StringUtils.isEmpty(authExpression)) {
      authExpression = actionConfig.getApplicationConfig().authExpression();
    }
    if (StringUtils.isEmpty(authExpression)) {
      log.warn("Action auth expression is set to empty, will not allow any request by default.");
      throw new ActionUnauthenticatedException();
    }
    val authority = compileAuthority(authExpression);
    processor = processor.andThen(user -> authority.accept(user.getPermissions(), user.getRoles()));
    return processor;
  }

  private static BiConsumer<Collection<String>, Collection<String>> compileAuthority(final String expression) {
    // TODO implement a complex expression parsing supporting AND/OR conditions, this is a single value version:
    if (expression.startsWith(ROLE_PREFIX)) {
      val roleName = expression.substring(ROLE_PREFIX.length());
      return (permissions, roles) -> {
        if (!roles.contains(roleName)) {
          throw new ActionUnauthorizedException();
        }
      };
    } else {
      return (permissions, roles) -> {
        if (!permissions.contains(expression)) {
          throw new ActionUnauthorizedException();
        }
      };
    }
  }

  @Override
  public void beforeAction(EdgeContext context) {
    val actionConfig = context.getActionConfig();
    Method actionMethod = actionConfig.getMethodSignature().getMethod();
    var processor = PROCESSOR_CACHE.get(actionMethod);
    if (processor == null) {
      processor = createProcessor(actionConfig);
      PROCESSOR_CACHE.put(actionMethod, processor);
    }
    processor.accept(context.currentUser());
  }
}
