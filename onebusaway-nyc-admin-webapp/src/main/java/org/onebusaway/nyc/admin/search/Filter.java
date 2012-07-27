package org.onebusaway.nyc.admin.search;

/**
 * Represents a filter that can be used  when searching a field
 * @author abelsare
 *
 */
public interface Filter<T> {
	
	/**
	 * Filters the given type based on the search criteria in the implementation
	 * @param type type of object to filter
	 * @return true if the type satisfies the search criteria, false otherwise
	 */
	boolean apply(T type);

}
