package com.innopals.edge.core;

import com.innopals.edge.SessionStore;
import com.innopals.edge.UserIdentity;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author bestmike007
 */
public class InMemorySessionStore implements SessionStore {
  @Override
  public UserIdentity getUserIdentity(@NotNull String sessionId) {
    return null;
  }

  @Override
  public void touch(@NotNull String sessionId, Date expire) {

  }

  @Override
  public void storeSession(@NotNull String sessionId, @NotNull UserIdentity userIdentity, Date expire) {

  }

  @Override
  public void removeSession(@NotNull String sessionId) {

  }
}
