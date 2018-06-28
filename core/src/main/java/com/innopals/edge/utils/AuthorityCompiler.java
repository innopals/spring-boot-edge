package com.innopals.edge.utils;

import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author bestmike007
 */
public final class AuthorityCompiler {

  private static final String ROLE_PREFIX = "role:";
  private static final int EXPECT_CHAR = 1;
  private static final int EXPECT_AND_OR = 2;
  private static final int EXPECT_LEFT_BRACKET = 4;
  private static final int EXPECT_RIGHT_BRACKET = 8;

  private static class ExpressionNode {
    private Boolean opAnd;
    private BiFunction<Collection<String>, Collection<String>, Boolean> authority;
    private StringBuilder builder;
  }

  private static BiFunction<Collection<String>, Collection<String>, Boolean> compileSimpleAuthority(@NotEmpty final String expression) {
    BiFunction<Collection<String>, Collection<String>, Boolean> authority;
    if (expression.startsWith(ROLE_PREFIX)) {
      val roleName = expression.substring(ROLE_PREFIX.length());
      authority = (permissions, roles) -> roles.contains(roleName);
    } else {
      authority = (permissions, roles) -> permissions.contains(expression);
    }
    return authority;
  }

  private static boolean compileNode(ExpressionNode current) {
    if (current.opAnd == null && current.builder != null && current.authority == null) {
      current.authority = compileSimpleAuthority(current.builder.toString().trim());
      current.builder = null;
      current.opAnd = null;
      return false;
    }
    if (current.opAnd != null && current.builder != null && current.authority != null) {
      val prevAuthority = current.authority;
      val authority = compileSimpleAuthority(current.builder.toString().trim());
      current.authority = current.opAnd ?
        (permissions, roles) -> prevAuthority.apply(permissions, roles) && authority.apply(permissions, roles) :
        (permissions, roles) -> prevAuthority.apply(permissions, roles) || authority.apply(permissions, roles);
      current.builder = null;
      current.opAnd = null;
      return false;
    }
    return current.opAnd != null || current.builder != null || current.authority == null;
  }

  public static BiFunction<Collection<String>, Collection<String>, Boolean> compileAuthority(final String expression) {
    if (StringUtils.isEmpty(expression)) {
      throw new RuntimeException("Empty auth express not allowed.");
    }
    Consumer<Character> throwError = c -> {
      throw new RuntimeException(String.format("Unable to parse expression %s, unexpected char %s", expression, c));
    };
    String exp = expression.trim()
      .replaceAll("\\s*&{1,2}\\s*", "&")
      .replaceAll("\\s*\\|{1,2}\\s*", "|");
    Stack<ExpressionNode> parser = new Stack<>();
    ExpressionNode current = new ExpressionNode();
    int expecting = EXPECT_CHAR | EXPECT_LEFT_BRACKET;
    for (char c : exp.toCharArray()) {
      switch (c) {
        case '&':
        case '|': {
          if ((expecting & EXPECT_AND_OR) == 0 || compileNode(current)) {
            throwError.accept(c);
          }
          current.opAnd = c == '&';
          expecting = EXPECT_CHAR | EXPECT_LEFT_BRACKET;
          break;
        }
        case '(': {
          if ((expecting & EXPECT_LEFT_BRACKET) == 0) {
            throwError.accept(c);
          }
          parser.push(current);
          current = new ExpressionNode();
          expecting = EXPECT_CHAR | EXPECT_LEFT_BRACKET;
          break;
        }
        case ')': {
          if ((expecting & EXPECT_RIGHT_BRACKET) == 0 || parser.size() == 0 || compileNode(current)) {
            throwError.accept(c);
          }
          ExpressionNode prev = parser.pop();
          if (prev.opAnd == null && prev.authority == null && prev.builder == null) {
            prev.authority = current.authority;
          } else if (prev.opAnd != null && prev.authority != null && prev.builder == null) {
            val prevAuthority = prev.authority;
            val authority = current.authority;
            prev.authority = prev.opAnd ?
              (permissions, roles) -> prevAuthority.apply(permissions, roles) && authority.apply(permissions, roles) :
              (permissions, roles) -> prevAuthority.apply(permissions, roles) || authority.apply(permissions, roles);
            prev.opAnd = null;
          } else {
            throwError.accept(c);
          }
          current = prev;
          expecting = EXPECT_AND_OR | EXPECT_RIGHT_BRACKET;
          break;
        }
        default: {
          if ((expecting & EXPECT_CHAR) == 0 || (current.opAnd == null) != (current.authority == null)) {
            throwError.accept(c);
          }
          if (current.builder == null) {
            current.builder = new StringBuilder();
          }
          current.builder.append(c);
          expecting = EXPECT_AND_OR | EXPECT_RIGHT_BRACKET | EXPECT_CHAR;
          break;
        }
      }
    }
    if (parser.size() > 0) {
      throw new RuntimeException(String.format("Invalid auth expression %s, expecting ')'", expression));
    }
    if (compileNode(current)) {
      throw new RuntimeException(String.format("Invalid auth expression %s", expression));
    }
    return current.authority;
  }
}
