package com.innopals.edge.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @param <T> page list item type
 * @author bestmike007
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {
  private List<T> list;
  private Integer size;
  private Integer pageSize;
  private Integer page;
}
