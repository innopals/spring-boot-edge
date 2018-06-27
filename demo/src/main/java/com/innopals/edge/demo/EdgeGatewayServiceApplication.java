package com.innopals.edge.demo;

import com.innopals.edge.annotations.EnableEdge;
import com.innopals.edge.demo.api.config.SwaggerConfiguration;
import com.innopals.edge.demo.api.controllers.DemoPipingController;
import com.innopals.edge.demo.api.controllers.MockRearController;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

/**
 * @author bestmike007
 */
@EnableEdge(
  baseUrl = "http://127.0.0.1:${server.port}/mock/"
)
@Import({
  // component scan increase startup time @ComponentScan(basePackageClasses = {EdgeGatewayServiceApplication.class})
  SwaggerConfiguration.class,
  DemoPipingController.class,
  MockRearController.class
})
public class EdgeGatewayServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(EdgeGatewayServiceApplication.class, args);
  }
}
