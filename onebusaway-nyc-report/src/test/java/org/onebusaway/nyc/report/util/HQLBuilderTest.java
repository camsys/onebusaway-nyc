package org.onebusaway.nyc.report.util;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.junit.Test;
import org.onebusaway.nyc.report.util.HQLBuilder;

/**
 * Tests {@link HQLBuilder}
 * @author abelsare
 *
 */
public class HQLBuilderTest {

	private HQLBuilder queryBuilder;
	
	@Test
	public void testSimpleQuery() {
		queryBuilder = new HQLBuilder();
		StringBuilder hql = queryBuilder.from(new StringBuilder(), "CcAndInferredLocationRecord");
		hql = queryBuilder.where(hql, "vehicleId", ":vehicleId");
		hql = queryBuilder.where(hql, "vehicleAgencyId", ":vehicleAgencyId");
		hql = queryBuilder.order(hql, "vehicleId", null);
		assertEquals("Expecting well formed hql", "from CcAndInferredLocationRecord where vehicleId= :vehicleId" +
				" and vehicleAgencyId= :vehicleAgencyId order by vehicleId ", hql.toString());
	}
	
	@Test
	public void testJoinQuery() {
		queryBuilder = new HQLBuilder();
		StringBuilder hql = queryBuilder.from(new StringBuilder(), "CcAndInferredLocationRecord", "cc");
		hql = queryBuilder.from(hql, "ArchivedInferredLocationRecord", "in");
		hql = queryBuilder.where(hql, "cc", "vehicleId", ":vehicleId");
		hql = queryBuilder.where(hql, "cc", "vehicleAgencyId", ":vehicleAgencyId");
		hql = queryBuilder.join(hql, "cc", "in", "vehicleId");
		hql = queryBuilder.order(hql, "cc", "vehicleId", "desc");
		assertEquals("Expecting well formed hql", "from CcAndInferredLocationRecord cc ," +
				"ArchivedInferredLocationRecord in where cc.vehicleId= :vehicleId" +
				" and cc.vehicleAgencyId= :vehicleAgencyId and cc.vehicleId = in.vehicleId " +
				"order by cc.vehicleId desc ", hql.toString());
	}
	
	@Test
	public void testSimpleJoinQuery() {
		queryBuilder = new HQLBuilder();
		StringBuilder hql = queryBuilder.from(new StringBuilder(), "CcAndInferredLocationRecord", "cc");
		hql = queryBuilder.from(hql, "ArchivedInferredLocationRecord", "in");
		hql = queryBuilder.join(hql, "cc", "in", "vehicleId");
		hql = queryBuilder.order(hql, "cc", "vehicleId", "desc");
		assertEquals("Expecting well formed hql", "from CcAndInferredLocationRecord cc ," +
				"ArchivedInferredLocationRecord in where " +
				"cc.vehicleId = in.vehicleId order by cc.vehicleId desc ", hql.toString());
	}
	
	@Test
	public void testDateBoundary() throws ParseException {
		queryBuilder = new HQLBuilder();
		
		StringBuilder hql = queryBuilder.from(new StringBuilder(), "CcAndInferredLocationRecord", "cc");
		hql = queryBuilder.from(hql, "ArchivedInferredLocationRecord", "in");
		hql = queryBuilder.where(hql, "cc", "vehicleId", ":vehicleId");
		hql = queryBuilder.where(hql, "cc", "vehicleAgencyId", ":vehicleAgencyId");
	    
		
		hql = queryBuilder.dateBoundary(hql, "cc", "timeReported", ":startDate", ":endDate");
		hql = queryBuilder.join(hql, "cc", "in", "vehicleId");
		hql = queryBuilder.order(hql, "cc", "vehicleId", "desc");
		
		assertEquals("Expecting well formed hql", "from CcAndInferredLocationRecord cc ," +
				"ArchivedInferredLocationRecord in where cc.vehicleId= :vehicleId" +
				" and cc.vehicleAgencyId= :vehicleAgencyId and( cc.timeReported >= :startDate and " +
				"cc.timeReported < :endDate)" +
				" and cc.vehicleId = in.vehicleId " +
				"order by cc.vehicleId desc ", hql.toString());
	}

}
