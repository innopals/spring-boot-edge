package com.innopals.edge;

import okhttp3.OkHttpClient;

/**
 * @author bestmike007
 */
public interface OkHttpClientConfigurer {
  /**
   * Custom configurer for okHttpClient
   * @param builder use the builder to configure okHttpClient
   */
  void configure(OkHttpClient.Builder builder);
}
