package com.innopals.edge.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author yrw
 * @since 2018/2/14
 */


@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = EdgeGatewayServiceApplication.class,
  webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
public class TrustedProxyTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void trustedProxyTest() throws Exception {
    mvc.perform(
      get("/public/api/v1/echo")
        .header("X-FORWARDED-FOR", "1.2.3.4, 192.168.1.1, 172.16.0.1")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("1000000"))
      .andExpect(jsonPath("$.data['X-FORWARDED-FOR']").value("1.2.3.4, 192.168.1.1, 172.16.0.1, 127.0.0.1"))
      .andExpect(jsonPath("$.data['X-REAL-IP']").value("1.2.3.4"));

    mvc.perform(
      get("/public/api/v1/echo")
        .header("X-FORWARDED-FOR", "5.6.7.8, 1.2.3.4, 192.168.1.1, 172.16.0.1")
        .header("X-REAL-IP", "5.6.7.8")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("1000000"))
      .andExpect(jsonPath("$.data['X-FORWARDED-FOR']").value("1.2.3.4, 192.168.1.1, 172.16.0.1, 127.0.0.1"))
      .andExpect(jsonPath("$.data['X-REAL-IP']").value("1.2.3.4"));
  }
}
