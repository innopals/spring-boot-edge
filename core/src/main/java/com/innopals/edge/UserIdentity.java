package com.innopals.edge;

import lombok.Data;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author bestmike007
 */
@Data
public class UserIdentity implements Serializable {
  /***
   * unique user id, empty if it's an unauthenticated user.
   */
  private String id;
  private Collection<String> roles;
  private Collection<String> permissions;
}
