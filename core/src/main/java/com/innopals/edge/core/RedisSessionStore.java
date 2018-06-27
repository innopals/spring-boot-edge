package com.innopals.edge.core;

import com.innopals.edge.SessionStore;
import com.innopals.edge.UserIdentity;

import javax.validation.constraints.NotNull;

/**
 * TODO implement default session store using redis template.
 * @author bestmike007
 */
public class RedisSessionStore implements SessionStore {
  @Override
  public UserIdentity getUserIdentity(@NotNull String sessionId) {
    return null;
  }

  @Override
  public void touch(@NotNull String sessionId, int secondsToExpire) {

  }

  @Override
  public void storeSession(@NotNull String sessionId, @NotNull UserIdentity userIdentity, int secondsToExpire) {

  }

  @Override
  public void removeSession(@NotNull String sessionId) {

  }
}
