package com.innopals.edge.demo.api.controllers;

import com.innopals.edge.EdgeContext;
import com.innopals.edge.UserIdentity;
import com.innopals.edge.annotations.AuthLevel;
import com.innopals.edge.annotations.EdgeAction;
import com.innopals.edge.annotations.EdgeController;
import com.innopals.edge.demo.api.domain.User;
import com.innopals.edge.domain.ResultWrapper;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author yrw
 * @since 2018/2/21
 */

@Validated
@EdgeController(
  authLevel = AuthLevel.AUTHENTICATED
)
@RestController
public class AuthController {

  @EdgeAction(
    authLevel = AuthLevel.UNAUTHENTICATED
  )
  @GetMapping("/api/v1/auth")
  public ResultWrapper<String> auth(EdgeContext context) {
    String sessionId = context.setCurrentUser(new UserIdentity() {{
      setId("1234");
      setRoles(Arrays.asList("users", "managers"));
      setPermissions(Arrays.asList("getUser", "listUsers"));
    }});
    return new ResultWrapper<>(sessionId);
  }

  @EdgeAction(
    backendPath = "/api/v1/users/{id}"
  )
  @GetMapping("/api/v1/users/{id}")
  public ResultWrapper<User> getUser(
    @ApiParam(value = "User id")
    @PathVariable("id") @Max(value = 100) Long id) {
    return null;
  }

  @EdgeAction(
    authLevel = AuthLevel.REQUIRE_AUTHORIZATION,
    authExpression = "role:users",
    backendPath = "/api/v1/users/{id}"
  )
  @GetMapping("/api/v1/users/{id}/testRole")
  public ResultWrapper<User> getUserWithRole(
    @ApiParam(value = "User id")
    @PathVariable("id") @Max(value = 100) Long id) {
    return null;
  }

  @EdgeAction(
    authLevel = AuthLevel.REQUIRE_AUTHORIZATION,
    authExpression = "getUser",
    backendPath = "/api/v1/users/{id}"
  )
  @GetMapping("/api/v1/users/{id}/testPermission")
  public ResultWrapper<User> getUserWithPermission(
    @ApiParam(value = "User id")
    @PathVariable("id") @Max(value = 100) Long id) {
    return null;
  }

  @EdgeAction(
    authLevel = AuthLevel.REQUIRE_AUTHORIZATION,
    authExpression = "role:admins",
    backendPath = "/api/v1/users/{id}"
  )
  @GetMapping("/api/v1/users/{id}/disallow")
  public ResultWrapper<User> getUserNotAllowed(
    @ApiParam(value = "User id")
    @PathVariable("id") @Max(value = 100) Long id) {
    return null;
  }

}
