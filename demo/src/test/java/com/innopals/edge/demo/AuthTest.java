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

import javax.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
public class AuthTest {

  @Autowired
  private MockMvc mvc;

  //200
  //json
  @Test
  public void basicAuthTest() throws Exception {

    mvc.perform(get("/api/v1/users/99")
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("401"));
    mvc.perform(get("/api/v1/users/99/disallow")
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("401"));

    MvcResult result = mvc.perform(get("/api/v1/auth")).andReturn();
    String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
    String body = result.getResponse().getContentAsString();
    assert setCookieHeader != null;
    assertThat(setCookieHeader).startsWith("s=");
    final String sessionId = setCookieHeader.substring(2);
    assertThat(body).isEqualTo("{\"code\":\"1000000\",\"message\":\"OK\",\"data\":\"" + sessionId + "\"}");

    mvc.perform(get("/api/v1/users/99")
      .cookie(new Cookie("s", sessionId))
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("1000000"))
      .andExpect(jsonPath("$.message").value("OK"))
      .andExpect(jsonPath("$.data.id").value(99))
      .andExpect(jsonPath("$.data.name").value("Name of 99")
      );

    mvc.perform(get("/api/v1/users/99/testRole")
      .cookie(new Cookie("s", sessionId))
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("1000000"))
      .andExpect(jsonPath("$.message").value("OK"))
      .andExpect(jsonPath("$.data.id").value(99))
      .andExpect(jsonPath("$.data.name").value("Name of 99")
      );

    mvc.perform(get("/api/v1/users/99/testPermission")
      .cookie(new Cookie("s", sessionId))
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("1000000"))
      .andExpect(jsonPath("$.message").value("OK"))
      .andExpect(jsonPath("$.data.id").value(99))
      .andExpect(jsonPath("$.data.name").value("Name of 99")
      );

    mvc.perform(get("/api/v1/users/99/disallow")
      .cookie(new Cookie("s", sessionId))
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("403"));
  }

}
