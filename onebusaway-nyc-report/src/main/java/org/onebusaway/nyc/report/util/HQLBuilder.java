package org.onebusaway.nyc.report.util;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class to build syntactically correct hql query string.
 * @author abelsare
 *
 */
public class HQLBuilder {

	private static final String SPACE = " ";
	
	/**
	 * Builds from statement for the entity with the given alias
	 * @param hqlBuilder hql query generated so far, not null
	 * @param entity entity from which records need to be selected
	 * @param alias alias for the entity
	 * @return hql query with from statement
	 */
	public StringBuilder from(StringBuilder hqlBuilder, String entity, String alias) {
		hqlBuilder  = from(hqlBuilder, entity);
		
		hqlBuilder.append(alias);
		hqlBuilder.append(SPACE);
		
		
		return hqlBuilder;
	}
	
	/**
	 * Builds from statement for the given entity. Use this method for simple (no join) queries
	 * @param hqlBuilder hql query generated so far, not null
	 * @param entity entity from which records need to be selected
	 * @return hql query with from statement
	 */
	public StringBuilder from(StringBuilder hqlBuilder, String entity) {
		String hql = hqlBuilder.toString();
		
		if(!(hql.contains("from"))) {
			hqlBuilder.append("from");
			hqlBuilder.append(SPACE);
			hqlBuilder.append(entity);
		} else {
			hqlBuilder.append(",");
			hqlBuilder.append(entity);
		}
		
		hqlBuilder.append(SPACE);
		
		return hqlBuilder;
	}
	
	/**
	 * Builds where clause without alias. Use this method for simple (no join) queries
	 * @param hqlBuilder hql query generated so far, not null
	 * @param field field that needs to be queried
	 * @param value value of the field that is being queried
	 * @return hql query with where clause
	 */
	public StringBuilder where(StringBuilder hqlBuilder, String field, String value) {
		if(hqlBuilder.toString().contains("where")) {
			hqlBuilder.append("and " +field + "= " +value);
		} else {
			hqlBuilder.append("where " +field + "= " +value);
		}

		hqlBuilder.append(SPACE);

		return hqlBuilder;
	}
	
	/**
	 * Builds where clause with alias. 
	 * @param hqlBuilder hql query generated so far, not null
	 * @param alias fields alias
	 * @param field field that needs to be queried
	 * @param value value of the field that is being queried
	 * @return hql query with where clause
	 */
	public StringBuilder where(StringBuilder hqlBuilder, String alias, String field, String value) {
		return where(hqlBuilder, alias + "." +field, value);
	}
	
	/**
	 * Builds order by clause without alias. Use this method for simple (no join) queries
	 * @param hqlBuilder hql query generated so far
	 * @param field field on which results should be ordered
	 * @param order order of the results, default is ascending
	 * @return hql query with order by clause
	 */
	public StringBuilder order(StringBuilder hqlBuilder, String field, String order) {
		hqlBuilder.append("order by " +field);
		hqlBuilder.append(SPACE);
		if(StringUtils.isNotBlank(order)) {
			hqlBuilder.append(order);
			hqlBuilder.append(SPACE);
		}
		
		return hqlBuilder;
	}
	
	/**
	 * Builds order by clause with alias. 
	 * @param hqlBuilder hql query generated so far
	 * @param field field on which results should be ordered
	 * @param order order of the results, default is ascending
	 * @return hql query with order by clause
	 */
	public StringBuilder order(StringBuilder hqlBuilder, String alias, String field, String order) {
		return order(hqlBuilder, alias + "." +field, order);
	}
	
	/**
	 * Adds join condition to the hql query
	 * @param hqlBuilder hql query generated so far
	 * @param primaryAlias alias of primary table
	 * @param secondaryAlias alias of secondary table
	 * @param joinColumn column on which join needs to be created
	 * @return hql with join condition
	 */
	public StringBuilder join(StringBuilder hqlBuilder, String primaryAlias, String secondaryAlias, 
			String joinColumn) {
		if(hqlBuilder.toString().contains("where")) {
			hqlBuilder.append("and").append(SPACE);
		} else {
			hqlBuilder.append("where").append(SPACE);
		}
		hqlBuilder.append(primaryAlias + "." +joinColumn);
		hqlBuilder.append(SPACE);
		hqlBuilder.append("=");
		hqlBuilder.append(SPACE);
		hqlBuilder.append(secondaryAlias + "." +joinColumn);
		hqlBuilder.append(SPACE);
		
		return hqlBuilder;
	}
	
	/**
	 * Builds hql query with date boundary. Checks whether given field falls within requied date
	 * boundary
	 * @param hqlBuilder hql query generated so far
	 * @param field filed that needs to be queried
	 * @param startDate date boundary lower bound
	 * @param endDate date boundary upper bound
	 * @return hql with date boundary
	 */
	public StringBuilder dateBoundary(StringBuilder hqlBuilder, String field, String startDate, 
			String endDate) {
		if(hqlBuilder.toString().contains("where")) {
			hqlBuilder.append("and(").append(SPACE);
		} else {
			hqlBuilder.append("where(").append(SPACE);
		}
		hqlBuilder.append(field).append(SPACE);
		hqlBuilder.append(">=").append(SPACE);
		hqlBuilder.append(startDate).append(SPACE);
		hqlBuilder.append("and").append(SPACE);
		hqlBuilder.append(field).append(SPACE);
		hqlBuilder.append("<").append(SPACE);
		hqlBuilder.append(endDate).append(")").append(SPACE);
		
		return hqlBuilder;
	}
	
	/**
	 * Builds hql query with date boundary with entity alias. Checks whether given field falls 
	 * within requied date boundary
	 * @param hqlBuilder hql query generated so far
	 * @param alias entity alias
	 * @param field filed that needs to be queried
	 * @param startDate date boundary lower bound
	 * @param endDate date boundary upper bound
	 * @return hql with date boundary
	 */
	public StringBuilder dateBoundary(StringBuilder hqlBuilder, String alias, String field,
			String startDate, String endDate) {
		return dateBoundary(hqlBuilder, alias + "." +field, startDate, endDate);
	}
	
}
