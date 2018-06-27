package com.innopals.edge.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @param <T> list item type
 * @author bestmike007
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListResult<T> {
	private List<T> list;
}
