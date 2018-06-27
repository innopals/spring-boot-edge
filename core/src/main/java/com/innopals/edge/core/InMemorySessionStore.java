package com.innopals.edge.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.innopals.edge.SessionStore;
import com.innopals.edge.UserIdentity;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

/**
 * @author bestmike007
 */
public class InMemorySessionStore implements SessionStore {

  private static final class ExpiringUserIdentity {
    private UserIdentity userIdentity;
    private long expire;
    private int secondsToExpire;
  }

  private final Cache<String, ExpiringUserIdentity> cache;

  public InMemorySessionStore(int maxSecondsToExpire) {
    cache = CacheBuilder.newBuilder()
      .expireAfterAccess(maxSecondsToExpire, TimeUnit.SECONDS)
      .build();
  }

  @Override
  public UserIdentity getUserIdentity(@NotNull String sessionId) {
    ExpiringUserIdentity expiringUserIdentity = cache.getIfPresent(sessionId);
    if (expiringUserIdentity == null) {
      return null;
    }
    if (expiringUserIdentity.expire > System.currentTimeMillis()) {
      expiringUserIdentity.expire = System.currentTimeMillis() + expiringUserIdentity.secondsToExpire * 1000;
      return expiringUserIdentity.userIdentity;
    } else {
      cache.invalidate(sessionId);
      return null;
    }
  }

  @Override
  public void touch(@NotNull String sessionId, int secondsToExpire) {
    ExpiringUserIdentity expiringUserIdentity = cache.getIfPresent(sessionId);
    if (expiringUserIdentity == null) {
      return;
    }
    expiringUserIdentity.secondsToExpire = secondsToExpire;
    expiringUserIdentity.expire = System.currentTimeMillis() + expiringUserIdentity.secondsToExpire * 1000;
  }

  @Override
  public void storeSession(@NotNull String sessionId, @NotNull UserIdentity userIdentity, int secondsToExpire) {
    ExpiringUserIdentity expiringUserIdentity = new ExpiringUserIdentity();
    expiringUserIdentity.userIdentity = userIdentity;
    expiringUserIdentity.secondsToExpire = secondsToExpire;
    expiringUserIdentity.expire = System.currentTimeMillis() + secondsToExpire * 1000;
    cache.put(sessionId, expiringUserIdentity);
  }

  @Override
  public void removeSession(@NotNull String sessionId) {
    cache.invalidate(sessionId);
  }
}
