package com.innopals.edge.core.actionprocessors;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innopals.edge.*;
import com.innopals.edge.annotations.BackendAuthType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import okhttp3.*;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * @author bestmike007
 */
@Slf4j
public class MapToBackendActionProcessor implements EdgeActionProcessor {

  @Data
  private static class BackendMapping {
    private String baseUrl;
    private Function<String, String> pathGetter;
    private String httpMethod;
    private BackendAuthType authType;
    private String authSecret;
    private Function<EdgeContext, Properties> pathPropertyResolver;
    private Function<EdgeContext, Object> requestBodyResolver;
    private Function<EdgeContext, String> authHeaderResolver;
  }

  private final static String HEADER_CONTENT_LENGTH = "Content-Length";
  private final static String HTTP_METHOD_GET = "GET";
  private final static String HTTP_METHOD_POST = "POST";
  private final static String HTTP_METHOD_PUT = "PUT";
  private final static String HTTP_METHOD_DELETE = "DELETE";
  private final static String PATH_SEPARATOR = "/";
  private final static WeakHashMap<Method, BackendMapping> MAPPING_CACHE = new WeakHashMap<>();
  private final static PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("{", "}");

  private final ObjectMapper objectMapper;
  private final OkHttpClient okHttpClient;

  public MapToBackendActionProcessor(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
    this.objectMapper = objectMapper;
    this.okHttpClient = okHttpClient;
  }

  private static String getMappingPath(Annotation requestMapping) {
    if (requestMapping == null) {
      return null;
    }
    try {
      Method valueMethod = requestMapping.getClass().getMethod("value");
      Method pathMethod = requestMapping.getClass().getMethod("path");
      String[] paths = (String[]) valueMethod.invoke(requestMapping);
      if (paths.length == 0) {
        paths = (String[]) pathMethod.invoke(requestMapping);
      }
      String path = paths.length > 0 ? paths[0] : null;
      if (path != null && path.startsWith(PATH_SEPARATOR)) {
        path = path.substring(1);
      }
      return path;
    } catch (Exception e) {
      return null;
    }
  }

  private static BackendMapping getMapping(EdgeContext context) {
    val actionConfig = context.getActionConfig();
    final Method actionMethod = actionConfig.getMethodSignature().getMethod();
    if (MAPPING_CACHE.containsKey(actionMethod)) {
      return MAPPING_CACHE.get(actionMethod);
    }
    BackendMapping mapping = new BackendMapping() {{
      // set base url
      String baseUrl = actionConfig.getActionConfig().baseUrl();
      if (StringUtils.isEmpty(baseUrl)) {
        baseUrl = actionConfig.getControllerConfig().baseUrl();
      }
      if (StringUtils.isEmpty(baseUrl)) {
        baseUrl = actionConfig.getApplicationConfig().baseUrl();
      }
      if (!baseUrl.endsWith(PATH_SEPARATOR)) {
        baseUrl = baseUrl + PATH_SEPARATOR;
      }
      setBaseUrl(baseUrl);
      // set backend http method
      if (StringUtils.isNotEmpty(actionConfig.getActionConfig().backendHttpMethod())) {
        setHttpMethod(actionConfig.getActionConfig().backendHttpMethod());
      }
      // set backend path getter
      if (StringUtils.isNotEmpty(actionConfig.getActionConfig().backendPath())) {
        String path = actionConfig.getActionConfig().backendPath();
        setPathGetter(httpMethod -> path);
      } else {
        final String getPath = getMappingPath(actionMethod.getAnnotation(GetMapping.class));
        final String postPath = getMappingPath(actionMethod.getAnnotation(PostMapping.class));
        final String putPath = getMappingPath(actionMethod.getAnnotation(PutMapping.class));
        final String deletePath = getMappingPath(actionMethod.getAnnotation(DeleteMapping.class));
        setPathGetter(httpMethod -> {
          if (StringUtils.equalsIgnoreCase(httpMethod, HTTP_METHOD_GET)) {
            return getPath;
          }
          if (StringUtils.equalsIgnoreCase(httpMethod, HTTP_METHOD_POST)) {
            return postPath;
          }
          if (StringUtils.equalsIgnoreCase(httpMethod, HTTP_METHOD_PUT)) {
            return putPath;
          }
          if (StringUtils.equalsIgnoreCase(httpMethod, HTTP_METHOD_DELETE)) {
            return deletePath;
          }
          return null;
        });
      }
      // set backend auth type
      BackendAuthType authType = actionConfig.getActionConfig().backendAuthType();
      if (authType == BackendAuthType.USE_PARENT) {
        authType = actionConfig.getControllerConfig().backendAuthType();
      }
      if (authType == BackendAuthType.USE_PARENT) {
        authType = actionConfig.getApplicationConfig().backendAuthType();
      }
      if (authType == BackendAuthType.USE_PARENT) {
        authType = BackendAuthType.NONE;
      }
      setAuthType(authType);
      // set backend auth secret
      String authSecret = actionConfig.getActionConfig().backendAuthSecret();
      if (StringUtils.isEmpty(authSecret)) {
        authSecret = actionConfig.getControllerConfig().backendAuthSecret();
      }
      if (StringUtils.isEmpty(authSecret)) {
        authSecret = actionConfig.getApplicationConfig().backendAuthSecret();
      }
      setAuthSecret(authSecret);
      // set auth header resolver
      if (authType == BackendAuthType.JWT_HMAC256) {
        Algorithm algorithm = Algorithm.HMAC256(getAuthSecret());
        setAuthHeaderResolver(context -> {
          Date nowDate = new Date();
          Date expire = DateUtils.addSeconds(nowDate, 5);
          String userId = context.currentUser().getId();
          JWTCreator.Builder builder = JWT.create()
            .withIssuedAt(new Date())
            .withExpiresAt(expire);
          if (StringUtils.isNotEmpty(userId)) {
            builder.withClaim("uid", userId);
          }
          return builder.sign(algorithm);
        });
      } else {
        setAuthHeaderResolver(ctx -> null);
      }
      // set path property resolver & request body resolver
      Annotation[][] parameterAnnotations = actionMethod.getParameterAnnotations();
      List<BiConsumer<Object[], Properties>> propertyResolvers = new LinkedList<>();
      for (int i = 0; i < parameterAnnotations.length; i++) {
        Annotation[] annotations = parameterAnnotations[i];
        PathVariable pathVariableAnnotation = (PathVariable) Arrays.stream(annotations).filter(PathVariable.class::isInstance).findAny().orElse(null);
        if (pathVariableAnnotation != null && !StringUtils.isAllEmpty(pathVariableAnnotation.name(), pathVariableAnnotation.value())) {
          final int index = i;
          final String key = StringUtils.isNotEmpty(pathVariableAnnotation.name()) ? pathVariableAnnotation.name() : pathVariableAnnotation.value();
          propertyResolvers.add((args, properties) -> properties.put(key, String.valueOf(args[index])));
        }
        if (Arrays.stream(annotations).anyMatch(RequestBody.class::isInstance)) {
          final int index = i;
          setRequestBodyResolver(context -> context.getActionArgs()[index]);
        }
      }
      setPathPropertyResolver(context -> {
        Properties properties = new Properties();
        for (val resolver : propertyResolvers) {
          resolver.accept(context.getActionArgs(), properties);
        }
        return properties;
      });
      if (getRequestBodyResolver() == null) {
        setRequestBodyResolver(ctx -> null);
      }
    }};
    MAPPING_CACHE.put(actionMethod, mapping);
    return mapping;
  }

  private Request buildRequest(EdgeContext context) throws Exception {
    val mapping = getMapping(context);
    val builder = new Request.Builder();
    // Get target url
    var backendUrl = mapping.getBaseUrl();
    var path = mapping.getPathGetter().apply(context.getRequest().getMethod());
    if (path == null) {
      throw new RuntimeException("Unable to resolve backend request path");
    }
    path = PLACEHOLDER_HELPER.replacePlaceholders(
      path, mapping.getPathPropertyResolver().apply(context)
    );
    backendUrl += path;
    var queryString = context.getRequest().getQueryString();
    if (StringUtils.isNotEmpty(queryString)) {
      backendUrl += "?" + queryString;
    }
    builder.url(backendUrl);
    // Determine backend request method & parse request body if needed.
    var httpMethod = mapping.getHttpMethod();
    if (StringUtils.isEmpty(httpMethod)) {
      httpMethod = context.getRequest().getMethod();
    }
    if (StringUtils.equalsIgnoreCase(httpMethod, HTTP_METHOD_POST) || StringUtils.equalsIgnoreCase(httpMethod, HTTP_METHOD_PUT)) {
      Object requestBody = mapping.getRequestBodyResolver().apply(context);
      builder.method(
        httpMethod,
        requestBody == null ? null : okhttp3.RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(requestBody))
      );
    }
    // generate auth header
    val backendAuthHeader = mapping.getAuthHeaderResolver().apply(context);
    val headerBuilder = new Headers.Builder();
    headerBuilder.add("X-FORWARDED-FOR", StringUtils.join(context.getForwardedIps(), ", "));
    headerBuilder.add("X-REAL-IP", context.getRealIp());
    if (StringUtils.isNotEmpty(backendAuthHeader)) {
      headerBuilder.add("Authorization", "Bearer " + backendAuthHeader);
    }
    // add extra request headers
    val headerNames = context.getRequest().getHeaderNames();
    while (headerNames.hasMoreElements()) {
      val name = headerNames.nextElement();
      if (StringUtils.equalsIgnoreCase(name, "Content-Type") ||
        StringUtils.equalsIgnoreCase(name, "Content-Length") ||
        StringUtils.equalsIgnoreCase(name, "X-FORWARDED-FOR") ||
        StringUtils.equalsIgnoreCase(name, "X-REAL-IP") ||
        StringUtils.equalsIgnoreCase(name, "Authorization")) {
        continue;
      }
      val values = context.getRequest().getHeaders(name);
      while (values.hasMoreElements()) {
        headerBuilder.add(name, values.nextElement());
      }
    }
    builder.headers(headerBuilder.build());
    return builder.build();
  }

  @Override
  public Object process(EdgeContext context, Object prevResult) throws Exception {
    if (prevResult != null) {
      return prevResult;
    }
    Request serviceRequest = buildRequest(context);
    Response serviceResponse = okHttpClient.newCall(serviceRequest).execute();
    if (serviceResponse.code() == 401) {
      throw new ActionUnauthenticatedException();
    } else if (serviceResponse.code() == 403) {
      throw new ActionUnauthorizedException();
    } else if (serviceResponse.code() == 429) {
      throw new ActionTooManyRequestsException();
    } else if (serviceResponse.code() >= 400) {
      ResponseBody body = serviceResponse.body();
      String msg = String.format(
        "Unexpected internal server response code %d, message: %s",
        serviceResponse.code(),
        body == null ? serviceResponse.message() : body.string()
      );
      log.warn(msg);
      throw new Exception(msg);
    }
    final HttpServletResponse response = context.getResponse();
    // pipe headers
    response.setStatus(serviceResponse.code());
    serviceResponse.headers().toMultimap().forEach((key, values) -> {
      if (StringUtils.equalsIgnoreCase(key, HEADER_CONTENT_LENGTH) ||
        StringUtils.startsWithIgnoreCase(key, "Access-Control-Allow-") ||
        context.getHideProxyResponseHeaders().stream().anyMatch(hide -> StringUtils.equalsIgnoreCase(key, hide))
        ) {
        return;
      }
      values.forEach(value -> response.addHeader(key, value));
    });
    context.getProxyResponseHeaderProcessors().forEach(processor -> processor.accept(response));
    // TODO check service response content type
    // pipe result.
    ResponseBody body = serviceResponse.isSuccessful() ? serviceResponse.body() : null;
    if (body != null) {
      if (context.getPostProcessor() != null && context.getBackendResultType() != null) {
        @SuppressWarnings("unchecked") Function<Supplier<Object>, Object> processor = (Function<Supplier<Object>, Object>) context.getPostProcessor();
        Object result = processor.apply(() -> {
          try {
            return objectMapper.readValue(body.byteStream(), context.getBackendResultType());
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        });
        response.getWriter().write(objectMapper.writeValueAsString(result));
      } else {
        StreamUtils.copy(body.byteStream(), response.getOutputStream());
      }
    }
    return null;
  }
}