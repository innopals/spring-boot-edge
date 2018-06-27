package com.innopals.edge.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author bestmike007
 */
@Slf4j
public final class AnnotationPlaceholderResolver {
  public static <T extends Annotation> T resolveAnnotation(Class<T> type, T source, Environment env) {
    return resolveAnnotation(type, source, env, null);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T resolveAnnotation(Class<T> type, T source, Environment env, String configPrefix) {
    if (StringUtils.isNotEmpty(configPrefix)) {
      log.warn("Config prefix is set to {} but will be ignored for now.", configPrefix);
    }
    return (T) Proxy.newProxyInstance(
      type.getClassLoader(),
      new Class[]{type},
      (proxy, method, methodArgs) -> {
        if (method.getReturnType().equals(String.class)) {
          return env.resolvePlaceholders((String) method.invoke(source));
        }
        if (method.getReturnType().equals(String[].class)) {
          String[] values = (String[]) method.invoke(source);
          List<String> parsed = new LinkedList<>();
          Arrays.stream(values).forEach(
            c -> Arrays.stream(env.resolvePlaceholders(c).split(",")).forEach(
              r -> parsed.add(r.trim())
            )
          );
        }
        return method.invoke(source);
      }
    );
  }
}
