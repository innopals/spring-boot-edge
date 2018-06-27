package com.innopals.edge.demo.api.controllers;

import com.innopals.edge.EdgeContext;
import com.innopals.edge.annotations.AuthLevel;
import com.innopals.edge.annotations.EdgeAction;
import com.innopals.edge.annotations.EdgeController;
import com.innopals.edge.demo.api.domain.User;
import com.innopals.edge.demo.api.domain.UserInfo;
import com.innopals.edge.domain.ListResult;
import com.innopals.edge.domain.PagedResult;
import com.innopals.edge.domain.ResultWrapper;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author yrw
 * @since 2018/2/21
 */

@Validated
@EdgeController(
  authLevel = AuthLevel.UNAUTHENTICATED
)
@RestController
@RequestMapping("/public")
public class DemoPipingController {

  @EdgeAction
  @GetMapping("/api/v1/users/{id}")
  public ResultWrapper<User> getUser(
    @ApiParam(value = "User id")
    @PathVariable("id") @Max(value = 100) Long id) {
    return null;
  }

  @EdgeAction
  @PostMapping("/api/v1/users")
  public ResultWrapper<User> createUser(
    @ApiParam(value = "User to be created") @RequestBody @Valid UserInfo user,
    BindingResult result) {
    return null;
  }

  @EdgeAction
  @PutMapping("/api/v1/users/{id}")
  public ResultWrapper<User> updateUser(
    @ApiParam(value = "User id") @PathVariable("id") Long id,
    @ApiParam(value = "Latest user info") @RequestBody UserInfo user
  ) {
    return null;
  }

  @EdgeAction
  @GetMapping("/api/v1/redirect")
  public void mockRedirect(
    @ApiParam(value = "The url to be redirected to") @RequestParam("url") String url,
    HttpServletResponse response) throws IOException {
    response.sendRedirect(url);
  }

  @EdgeAction
  @GetMapping("/api/v1/error")
  public ResultWrapper<String> generateError() {
    return null;
  }

  @EdgeAction
  @GetMapping("/api/v1/test/download")
  public void fileDownload(EdgeContext context) {
    context.setHeader("Content-Type", "image/png");
    context.setHeader("Content-Disposition", "attachment;filename=baidu-logo.png");
  }


  @EdgeAction
  @GetMapping("/api/v1/test/page")
  public ResultWrapper<PagedResult<String>> getPage() {
    return null;
  }

  @EdgeAction
  @GetMapping("/api/v1/test/page/list")
  public ResultWrapper<PagedResult<List<String>>> getPageList() {
    return null;
  }

  @EdgeAction
  @GetMapping("/api/v1/test/list")
  public ResultWrapper<ListResult<User>> getList() {
    return null;
  }

  @EdgeAction
  @GetMapping("/api/v1/test/list/map")
  public ResultWrapper<ListResult<Map<String, String>>> getListMap() {
    return null;
  }

}
