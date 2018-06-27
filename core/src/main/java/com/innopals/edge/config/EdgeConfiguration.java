package com.innopals.edge.config;

import com.innopals.edge.EdgeContext;
import com.innopals.edge.SessionIdResolver;
import com.innopals.edge.annotations.EnableEdge;
import com.innopals.edge.core.AnnotationPlaceholderResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author bestmike007
 * @since 2018/6/22
 */
@SuppressWarnings("SpringFacetCodeInspection")
@Configuration

public class EdgeConfiguration implements ApplicationContextAware {

  private ApplicationContext currentApplicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    currentApplicationContext = applicationContext;
  }

  @Bean
  public WebMvcConfigurer configureSpringMvc() {
    // resolve EdgeContext for edge controllers, always null because it will be handled using aop to get other method arguments.
    return new WebMvcConfigurer() {
      @Override
      public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        //noinspection NullableProblems
        argumentResolvers.add(new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(EdgeContext.class);
          }

          @Override
          public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
          ) {
            return null;
          }
        });
      }
    };
  }

  @Bean
  public EnableEdge edgeApplicationConfig(@Autowired Environment env) {
    Map<String, Object> beans = currentApplicationContext.getBeansWithAnnotation(EnableEdge.class);
    for (String name : beans.keySet()) {
      EnableEdge config = currentApplicationContext.findAnnotationOnBean(name, EnableEdge.class);
      if (config != null) {
        return AnnotationPlaceholderResolver.resolveAnnotation(EnableEdge.class, config, env);
      }
    }
    throw new RuntimeException("Unable to find the main class with EnableEdge annotation.");
  }

  @Bean
  public SessionIdResolver configSessionIdResolver(
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") @Autowired EnableEdge applicationConfig
  ) {
    final Function<HttpServletRequest, String> sessionIdResolver;
    final String bearerHeaderPrefix = "Bearer ";
    Function<HttpServletRequest, String> headerSessionIdResolver = applicationConfig.headerAuthorization() ? request -> {
      String header = request.getHeader("Authorization");
      if (header == null) {
        return null;
      }
      if (!header.startsWith(bearerHeaderPrefix)) {
        return null;
      }
      return header.substring(7).trim();
    } : null;
    Function<HttpServletRequest, String> cookieSessionIdResolver = StringUtils.isNotBlank(applicationConfig.cookieSessionIdKey()) ? request -> {
      if (request.getCookies() == null) {
        return null;
      }
      return Arrays.stream(request.getCookies()).filter(
        cookie -> StringUtils.equals(cookie.getName(), applicationConfig.cookieSessionIdKey())
      ).findFirst().map(Cookie::getValue).orElse(null);
    } : null;
    if (headerSessionIdResolver != null && cookieSessionIdResolver != null) {
      sessionIdResolver = request -> {
        // cookie resolver over header resolver
        String sessionId = cookieSessionIdResolver.apply(request);
        if (StringUtils.isBlank(sessionId)) {
          sessionId = headerSessionIdResolver.apply(request);
        }
        return sessionId;
      };
    } else if (cookieSessionIdResolver != null) {
      sessionIdResolver = cookieSessionIdResolver;
    } else if (headerSessionIdResolver != null) {
      sessionIdResolver = headerSessionIdResolver;
    } else {
      sessionIdResolver = request -> null;
    }
    return sessionIdResolver::apply;
  }
}