package com.innopals.edge;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.validation.constraints.NotNull;

/**
 * @author bestmike007
 */
public interface EdgeObjectMapperFactory {
  /**
   * @return An object mapper for edge to parse service result and serialize api output.
   */
  @NotNull ObjectMapper createObjectMapper();
}
