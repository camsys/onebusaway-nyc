package org.onebusaway.nyc.report_archive.util;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

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
		hql = queryBuilder.where(hql, "vehicleId", 321);
		hql = queryBuilder.where(hql, "vehicleAgencyId", "MTA_NYCT");
		hql = queryBuilder.order(hql, "vehicleId", null);
		assertEquals("Expecting well formed hql", "from CcAndInferredLocationRecord where vehicleId=321" +
				" and vehicleAgencyId='MTA_NYCT' order by vehicleId ", hql.toString());
	}
	
	@Test
	public void testJoinQuery() {
		queryBuilder = new HQLBuilder();
		StringBuilder hql = queryBuilder.from(new StringBuilder(), "CcAndInferredLocationRecord", "cc");
		hql = queryBuilder.from(hql, "ArchivedInferredLocationRecord", "in");
		hql = queryBuilder.where(hql, "cc", "vehicleId", 321);
		hql = queryBuilder.where(hql, "cc", "vehicleAgencyId", "MTA_NYCT");
		hql = queryBuilder.join(hql, "cc", "in", "vehicleId", "=");
		hql = queryBuilder.order(hql, "cc", "vehicleId", "desc");
		assertEquals("Expecting well formed hql", "from CcAndInferredLocationRecord cc ," +
				"ArchivedInferredLocationRecord in where cc.vehicleId=321" +
				" and cc.vehicleAgencyId='MTA_NYCT' and cc.vehicleId = in.vehicleId " +
				"order by cc.vehicleId desc ", hql.toString());
	}
	
	@Test
	public void testSimpleJoinQuery() {
		queryBuilder = new HQLBuilder();
		StringBuilder hql = queryBuilder.from(new StringBuilder(), "CcAndInferredLocationRecord", "cc");
		hql = queryBuilder.from(hql, "ArchivedInferredLocationRecord", "in");
		hql = queryBuilder.join(hql, "cc", "in", "vehicleId", "=");
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
		hql = queryBuilder.where(hql, "cc", "vehicleId", 321);
		hql = queryBuilder.where(hql, "cc", "vehicleAgencyId", "MTA_NYCT");
	    /*Date startDate = new Date(formatter.parseDateTime("2012-07-07 03:00:00").getMillis());
	    Date endDate = new Date(formatter.parseDateTime("2012-07-08 03:00:00").getMillis());*/
		hql = queryBuilder.dateBoundary(hql, "cc", "timeReported", "2012-07-07 03:00:00", "2012-07-08 03:00:00");
		hql = queryBuilder.join(hql, "cc", "in", "vehicleId", "=");
		hql = queryBuilder.order(hql, "cc", "vehicleId", "desc");
		assertEquals("Expecting well formed hql", "from CcAndInferredLocationRecord cc ," +
				"ArchivedInferredLocationRecord in where cc.vehicleId=321" +
				" and cc.vehicleAgencyId='MTA_NYCT' and( cc.timeReported between '2012-07-07 03:00:00' and '2012-07-08 03:00:00')" +
				" and cc.vehicleId = in.vehicleId " +
				"order by cc.vehicleId desc ", hql.toString());
	}

}
