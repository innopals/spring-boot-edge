package com.innopals.edge.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.innopals.edge.*;
import com.innopals.edge.annotations.EnableEdge;
import com.innopals.edge.domain.ResultWrapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.OkHttpClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolationException;

/**
 * @author bestmike007
 */

@Component
@Aspect
@Slf4j
public class EdgeControllerAdvice {

  private final Environment env;
  private final SessionStore sessionStore;
  private final SessionIdResolver sessionIdResolver;
  private final EnableEdge applicationConfig;
  private final EdgeActionPreProcessor preProcessor;
  private final EdgeActionProcessor processor;
  private final EdgeActionPreProcessor defaultPreProcessor;
  private final ObjectMapper objectMapper;

  public EdgeControllerAdvice(
    @Autowired Environment env,
    @Autowired(required = false) SessionStore sessionStore,
    @Autowired SessionIdResolver sessionIdResolver,
    @Autowired EnableEdge applicationConfig,
    @Autowired(required = false) EdgeActionProcessor processor,
    @Autowired(required = false) EdgeObjectMapperFactory objectMapperFactory,
    @Autowired(required = false) OkHttpClientConfigurer okHttpClientConfigurer,
    @Autowired(required = false) EdgeActionPreProcessor preProcessor) {
    this.env = env;
    this.sessionIdResolver = sessionIdResolver;
    this.applicationConfig = applicationConfig;
    this.preProcessor = preProcessor;
    if (objectMapperFactory == null) {
      // default edge object mapper
      objectMapper = new ObjectMapper();
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.registerModule(new JavaTimeModule());
    } else {
      objectMapper = objectMapperFactory.createObjectMapper();
    }
    if (processor == null) {
      val builder = new OkHttpClient.Builder();
      if (okHttpClientConfigurer != null) {
        okHttpClientConfigurer.configure(builder);
      }
      // Will never follow redirect, raw http code needed from the backend.
      builder.followRedirects(false);
      builder.followSslRedirects(false);
      processor = new DefaultEdgeActionProcessor(objectMapper, builder.build());
    }
    this.processor = processor;
    this.sessionStore = sessionStore == null ? new InMemorySessionStore(applicationConfig.sessionExpireInSeconds()) : sessionStore;
    this.defaultPreProcessor = new DefaultEdgeActionPreProcessor(applicationConfig);
  }

  @Around("@annotation(com.innopals.edge.annotations.EdgeAction)")
  public Object aroundEdgeAction(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!MethodSignature.class.isInstance(joinPoint.getSignature())) {
      // Not sure when this would happen, it will not be kept the original way.
      return joinPoint.proceed();
    }
    // Step 1: Resolve application level config, control level config & action level config
    EdgeActionConfig config = new EdgeActionConfig(env, applicationConfig, (MethodSignature) joinPoint.getSignature());
    EdgeContext context = new EdgeContext(sessionStore, sessionIdResolver, config);

    // Step 2: Set edge context if required in the action method argument list
    int contextIndex = config.getContextArgIndex();
    val actionArgs = joinPoint.getArgs();
    if (contextIndex >= 0 && contextIndex < actionArgs.length && actionArgs[contextIndex] == null) {
      actionArgs[contextIndex] = context;
    }
    context.setActionArgs(actionArgs);
    try {
      // Step 3. Auth & pre-process.
      defaultPreProcessor.beforeAction(context);
      if (preProcessor != null) {
        preProcessor.beforeAction(context);
      }

      // Step 4. Invoke the action method to get the final execution config
      Object result = joinPoint.proceed(actionArgs);

      // Step 5. Process & exception handling
      return processor.process(context, result);
    } catch (ActionTooManyRequestsException ex) {
      val response = context.getResponse();
      if (applicationConfig.wrapException()) {
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new ResultWrapper<String>() {{
          setCode(applicationConfig.tooManyRequestsExceptionCode());
          setMessage("Too many requests");
        }}));
      } else {
        response.sendError(429, "Too many requests");
      }
      return null;
    } catch (ActionUnauthenticatedException ex) {
      val response = context.getResponse();
      if (applicationConfig.wrapException()) {
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new ResultWrapper<String>() {{
          setCode(applicationConfig.unauthenticatedExceptionCode());
          setMessage("Not authenticated");
        }}));
      } else {
        response.sendError(401, "Not authenticated");
      }
      return null;
    } catch (ActionUnauthorizedException ex) {
      val response = context.getResponse();
      if (applicationConfig.wrapException()) {
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new ResultWrapper<String>() {{
          setCode(applicationConfig.unauthorizedExceptionCode());
          setMessage("Not authorized");
        }}));
      } else {
        response.sendError(403, "Not authorized");
      }
      return null;
    } catch (ConstraintViolationException ex) {
      val response = context.getResponse();
      val delimiter = ':';
      String msg = ex.getMessage();
      if (msg.indexOf(delimiter) >= 0) {
        msg = msg.substring(msg.indexOf(delimiter) + 1).trim();
      }
      if (applicationConfig.wrapException()) {
        val errMsg = msg;
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new ResultWrapper<String>() {{
          setCode(applicationConfig.illegalArgumentExceptionCode());
          setMessage(errMsg);
        }}));
      } else {
        response.sendError(400, msg);
      }
      return null;
    } catch (Exception ex) {
      log.error("Uncaught exception", ex);
      val response = context.getResponse();
      String msg = StringUtils.isEmpty(ex.getMessage()) ? "Server internal error" : ex.getMessage();
      if (applicationConfig.wrapException()) {
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new ResultWrapper<String>() {{
          setCode(applicationConfig.uncaughtExceptionCode());
          setMessage(msg);
        }}));
      } else {
        response.sendError(500, msg);
      }
      return null;
    }
  }

}