package com.innopals.edge.demo.api.config;

import com.innopals.edge.EdgeContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


/**
 * @author Diamond
 */
@Configuration
@EnableSwagger2
@Slf4j
@SuppressWarnings("SpringFacetCodeInspection")
public class SwaggerConfiguration implements WebMvcConfigurer {

  private final static String SWAGGER_UI_HEADER = "Referer";
  private final static String PROTOCOL = "http";
  private final static String SWAGGER_UI_URL_HOST = "petstore.swagger.io";

  /**
   * 初始化文档信息
   *
   * @return 文档信息
   */
  private ApiInfo initApiInfo() {
    Contact contact = new Contact("后端团队", "", "");
    return new ApiInfo(
      "test",
      "test",
      "1.0",
      "",
      contact,
      "Apache 2.0",
      "http://www.apache.org/licenses/LICENSE-2.0",
      new ArrayList<>()
    );
  }

  @Bean
  public Docket api() {
    Docket docket = new Docket(DocumentationType.SWAGGER_2);
    return docket
      .apiInfo(initApiInfo())
      .protocols(Collections.singleton(PROTOCOL))
      .ignoredParameterTypes(EdgeContext.class)
      .select()
      .apis(RequestHandlerSelectors.basePackage("com.innopals.edge.demo.api.controllers"))
      .paths(PathSelectors.any())
      .build();
  }

  @Bean
  public OncePerRequestFilter swaggerCorsFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String referer = request.getHeader(SWAGGER_UI_HEADER);
        if (StringUtils.isNotEmpty(referer) && referer.contains(SWAGGER_UI_URL_HOST)) {
          response.addHeader(
            "Access-Control-Allow-Origin",
            String.format("%s://%s", PROTOCOL, SWAGGER_UI_URL_HOST)
          );
          response.addHeader(
            "Access-Control-Allow-Headers",
            "DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Content-Range,Range,Authentication"
          );
          response.addHeader(
            "Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, PATCH, OPTIONS"
          );
        }
        filterChain.doFilter(request, response);
      }
    };
  }
}
