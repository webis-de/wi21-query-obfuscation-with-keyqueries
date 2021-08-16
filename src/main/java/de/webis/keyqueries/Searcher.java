package de.webis.keyqueries;

import java.util.List;

public interface Searcher<T> {
	public List<String> search(T query, int size);
}
