package com.innopals.edge.demo.api.controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.innopals.edge.demo.api.domain.User;
import com.innopals.edge.demo.api.domain.UserInfo;
import com.innopals.edge.domain.ListResult;
import com.innopals.edge.domain.PagedResult;
import com.innopals.edge.domain.ResultWrapper;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author bestmike007
 */
@RestController
@RequestMapping("mock")
public class MockRearController {

  @GetMapping("/api/v1/error")
  public ResultWrapper<String> clientError(HttpServletResponse response) throws Exception {
    response.sendError(500, "You should see this error message.");
    return null;
  }

  @GetMapping("/api/v1/users/{id}")
  public ResultWrapper<User> getUser(
    @ApiParam(value = "User id")
    @PathVariable("id") Long id
  ) {
    return new ResultWrapper<>(new User() {{
      setId(id);
      setName(String.format("Name of %d", id));
    }});
  }

  @PostMapping("/api/v1/users")
  public ResultWrapper<User> createUser(
    @ApiParam(value = "User to be created") @RequestBody UserInfo user
  ) {
    return new ResultWrapper<>(new User() {{
      setName(user.getName());
      setId(System.currentTimeMillis());
    }});
  }

  @PutMapping("/api/v1/users/{id}")
  public ResultWrapper<User> updateUser(
    @ApiParam(value = "User id") @PathVariable("id") Long id,
    @ApiParam(value = "Latest user info") @RequestBody UserInfo user
  ) {
    return new ResultWrapper<>(new User() {{
      setName(user.getName());
      setId(id);
    }});
  }

  @GetMapping("/api/v1/redirect")
  public void mockRedirect(
    @ApiParam(value = "The url to be redirected to") @RequestParam("url") String url,
    HttpServletResponse response) throws IOException {
    response.sendRedirect(url);
  }

  @GetMapping(value = "/api/v1/test/download", produces = "image/png")
  public void fileDownload(HttpServletResponse response)
    throws Exception {
    ByteSource content = Resources.asByteSource(new URL("https://www.baidu.com/img/bd_logo1.png"));
    response.setContentType("image/png");
    content.copyTo(response.getOutputStream());
  }

  @GetMapping("/api/v1/test/page")
  public ResultWrapper<PagedResult<String>> pageTest() {
    return new ResultWrapper<>(new PagedResult<String>() {{
      setPage(1);
      setPageSize(20);
      setSize(100);
      setList(Collections.singletonList("Test"));
    }});
  }

  @GetMapping("/api/v1/test/page/list")
  public ResultWrapper<PagedResult<List<String>>> pageListTest() {
    return new ResultWrapper<>(new PagedResult<List<String>>() {{
      setPage(1);
      setPageSize(20);
      setSize(100);
      setList(Collections.singletonList(Collections.singletonList("test")));
    }});

  }

  @GetMapping("/api/v1/test/list")
  public ResultWrapper<ListResult<User>> listTest() {
    return new ResultWrapper<>(new ListResult<User>() {{
      setList(Collections.singletonList(
        new User() {{
          setId(System.currentTimeMillis());
          setName("Test");
        }}
      ));
    }});
  }

  @GetMapping("/api/v1/test/list/map")
  public ResultWrapper<ListResult<Map<String, String>>> listMapTest() {
    return new ResultWrapper<>(new ListResult<Map<String, String>>() {{
      setList(Collections.singletonList(
        ImmutableMap.of(
          "key", "test"
        )
      ));
    }});
  }

  @GetMapping("/api/v1/user/{id}/authenticate")
  public ResultWrapper<User> authenticateUser(
    @ApiParam(value = "User id")
    @PathVariable("id") Long id
  ) {
    return new ResultWrapper<User>() {{
      setMessage("OK");
      setCode("1000000");
      setData(new User() {{
        setId(id);
        setName("Name of " + id);
      }});
    }};
  }

}
