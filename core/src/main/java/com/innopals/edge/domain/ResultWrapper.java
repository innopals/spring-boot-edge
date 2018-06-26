package com.innopals.edge.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @param <T> wrapped result type
 * @author bestmike007
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultWrapper<T> {
  private String code;
  private String message;
  private T data;

  @SuppressWarnings("unchecked")
  public ResultWrapper(T data) {
    this.code = "1000000";
    this.message = "OK";
    this.data = data;
  }
}
