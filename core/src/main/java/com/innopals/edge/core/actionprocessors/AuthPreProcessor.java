package com.innopals.edge.core.actionprocessors;

import com.innopals.edge.*;
import com.innopals.edge.annotations.AuthLevel;
import com.innopals.edge.core.EdgeActionConfig;
import com.innopals.edge.utils.AuthorityCompiler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The opinionated authentication and authorization process for edge configured by edge annotations.
 *
 * @author bestmike007
 */
@Slf4j
public class AuthPreProcessor implements EdgeActionPreProcessor {

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
      return user -> {
      };
    }
    Consumer<UserIdentity> processor = user -> {
      if (StringUtils.isEmpty(user.getId())) {
        throw new ActionUnauthenticatedException();
      }
    };
    // Authorization
    String authExpression = actionConfig.getActionConfig().authExpression();
    if (StringUtils.isEmpty(authExpression)) {
      authExpression = actionConfig.getControllerConfig().authExpression();
    }
    if (StringUtils.isEmpty(authExpression)) {
      authExpression = actionConfig.getApplicationConfig().authExpression();
    }
    if (authLevel == AuthLevel.AUTHENTICATED && StringUtils.isNotEmpty(authExpression)) {
      authLevel = AuthLevel.REQUIRE_AUTHORIZATION;
    }
    if (authLevel != AuthLevel.REQUIRE_AUTHORIZATION) {
      return processor;
    }
    if (StringUtils.isEmpty(authExpression)) {
      log.warn("Action auth expression is set to empty, will not allow any request by default.");
      throw new ActionUnauthenticatedException();
    }
    BiFunction<Collection<String>, Collection<String>, Boolean> authority = AuthorityCompiler.compileAuthority(authExpression);
    processor = processor.andThen(user -> {
      if (!authority.apply(user.getPermissions(), user.getRoles())) {
        throw new ActionUnauthorizedException();
      }
    });
    return processor;
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
