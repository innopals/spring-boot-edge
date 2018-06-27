package com.innopals.edge;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author bestmike007
 */
public interface SessionStore {
  /***
   * Get user identity by session id, return null if session id is invalid or expired.
   * @param sessionId the session id
   * @return UserIdentity or null
   */
  UserIdentity getUserIdentity(@NotNull String sessionId);

  /***
   * prolong the life-span of a session
   * @param sessionId the session id
   * @param secondsToExpire
   */
  void touch(@NotNull String sessionId, int secondsToExpire);

  /***
   * Save a session in the store.
   * @param sessionId the session id
   * @param userIdentity the user identity
   * @param secondsToExpire expire date of the session
   */
  void storeSession(@NotNull String sessionId, @NotNull UserIdentity userIdentity, int secondsToExpire);

  /***
   * Instantly invalidate the session
   * @param sessionId the session id
   */
  void removeSession(@NotNull String sessionId);
}
