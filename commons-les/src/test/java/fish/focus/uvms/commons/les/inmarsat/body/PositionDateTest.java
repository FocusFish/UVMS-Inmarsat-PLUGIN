package fish.focus.uvms.commons.les.inmarsat.body;

import fish.focus.uvms.commons.les.inmarsat.InmarsatDefinition;
import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Calendar;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@SuppressWarnings("UnnecessaryLocalVariable")
public class PositionDateTest {

	@Test
	public void getPositionDateDefault() throws Exception {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 1;
		int nowHour = 0;
		int nowMin = 2; // Must be in power of 2...

		int posYear = nowYear;
		int posMonth = nowMonth;
		int posDay = nowDay;
		int posHour = nowHour;
		int posMin = nowMin / 2;

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(posDay, posHour, posMin, null);
		PositionDate positionDateSpy = Mockito.spy(positionDate);
		when(positionDateSpy.getNowUtc()).thenReturn(now);
		Calendar expectedDate = createCalendarUTC(posYear, posMonth, posDay, posHour, posMin * 2);
		assertEquals(expectedDate.getTime(), positionDateSpy.getDate());

	}

	@Test
	public void getPositionDateNow() throws Exception {

		int nowYear = 2017;
		int nowMonth = Calendar.JANUARY;
		int nowDay = 1;
		int nowHour = 0;
		int nowMin = 2;// Must be in power of 2...

		int posMin = nowMin / 2;

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(nowDay, nowHour, posMin, null);
		Calendar expected = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));
	}

	@Test
	public void getPositionDateNow_ADayAfterNow() throws Exception {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 1;
		int nowHour = 0;
		int nowMin = 2; // Must be in power of 2...

		int posMonth = Calendar.FEBRUARY;
		int posDay = nowDay + 1; // After current day
		int posMin = nowMin / 2;

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(posDay, nowHour, posMin, null);
		Calendar expected = createCalendarUTC(nowYear, posMonth, posDay, nowHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));
	}

	@Test
	public void getPositionDateNow_HourAfterNow() throws Exception {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 1;
		int nowHour = 0;
		int nowMin = 2; // Must be in power of 2...

		int posMonth = nowMonth - 1;
		int posHour = nowHour + 1;
		int posMin = nowMin / 2;

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(nowDay, posHour, posMin, null);
		Calendar expected = createCalendarUTC(nowYear, posMonth, nowDay, posHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));
	}

	@Test
	public void getPositionDateMinAfterNow() throws Exception {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 1;
		int nowHour = 0;
		int nowMin = 2; // Must be in power of 2...

		int posMonth = nowMonth - 1;
		int posMin = (nowMin + 2) / 2;

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(nowDay, nowHour, posMin, null);
		Calendar expected = createCalendarUTC(nowYear, posMonth, nowDay, nowHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));
	}

	@Test
	public void getPositionDateInDayLeapYearDay() throws Exception {
		int nowYear = 2016;
		int nowMonth = Calendar.FEBRUARY;
		int nowDay = 29;
		int nowHour = 0;
		int nowMin = 2;// Must be in power of 2...

		int posMin = nowMin / 2;

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(nowDay, nowHour, posMin, null);
		Calendar expected = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));
	}

	@Test
	public void getPositionDateInDayNotValidInPreviousMonth() throws Exception {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 31;
		int nowHour = 0;
		int nowMin = 2;// Must be in power of 2...

		final int posMonth = Calendar.JANUARY;
		final int posHour = nowHour + 1;
		final int posMin = nowMin / 2;

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(nowDay, posHour, posMin, null);
		Calendar expected = createCalendarUTC(nowYear, posMonth, nowDay, posHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));
	}

	@Test
	public void getDateFromDateExtraFormat_1() throws InmarsatException {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 31;
		int nowHour = 0;
		int nowMin = 2;// Must be in power of 2...

		final int posHour = nowHour + 1;
		final int posMin = nowMin / 2;

		final int extraYear = 2016 - PositionDate.PositionDateExtra.FORMAT1_YEARSTART; // 0-63
		final int extraMonth = Calendar.FEBRUARY + 1; // 1-12

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate =
				new PositionDate(nowDay, posHour, posMin, new PositionDate.PositionDateExtra(extraYear, extraMonth));
		assertTrue(positionDate.getExtraDate().validate());
		Calendar expected = createCalendarUTC(extraYear + PositionDate.PositionDateExtra.FORMAT1_YEARSTART,
				extraMonth - 1, nowDay, posHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));

	}

	@Test
	public void getDateFromDateExtraFormat_2() throws InmarsatException {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 31;
		int nowHour = 0;
		int nowMin = 2;// Must be in power of 2...

		final int posHour = nowHour + 1;
		final int posMin = nowMin / 2;

		final int extraYear = 2016 - PositionDate.PositionDateExtra.FORMAT1_YEARSTART; // 0-63
		final int extraMonth = Calendar.DECEMBER + 1; // 1-12
		final int extraDay = 28; // 1-31
		final int extraHour = 2; // 0-23
		final int extraMin = 3; // 1-59

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(nowDay, posHour, posMin,
				new PositionDate.PositionDateExtra(2, extraYear, extraMonth, extraDay, extraHour, extraMin));
		assertTrue(positionDate.getExtraDate().validate());
		Calendar expected = createCalendarUTC(extraYear + PositionDate.PositionDateExtra.FORMAT2_YEARSTART,
				extraMonth - 1, nowDay, posHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));

	}

	@Test
	public void getDateFromDateExtraFormat_3() throws InmarsatException {
		int nowYear = 2017;
		int nowMonth = Calendar.MARCH;
		int nowDay = 31;
		int nowHour = 0;
		int nowMin = 2;// Must be in power of 2...

		final int posHour = nowHour + 1;
		final int posMin = nowMin / 2;

		final int extraYear = 2016; // 0-63
		final int extraMonth = Calendar.FEBRUARY + 1; // 1-12
		final int extraDay = 28; // 1-31
		final int extraHour = 2; // 0-23
		final int extraMin = 3; // 1-59

		Calendar now = createCalendarUTC(nowYear, nowMonth, nowDay, nowHour, nowMin);
		PositionDate positionDate = new PositionDate(nowDay, posHour, posMin,
				new PositionDate.PositionDateExtra(3, extraYear, extraMonth, extraDay, extraHour, extraMin));
		assertTrue(positionDate.getExtraDate().validate());
		Calendar expected = createCalendarUTC(extraYear, extraMonth - 1, nowDay, posHour, posMin * 2);
		assertEquals(expected.getTime(), positionDate.getDate(now));

	}

	private Calendar createCalendarUTC(final int nowYear, final int nowMonth, final int nowDay, final int nowHour,
			final int nowMin) {
		Calendar now = Calendar.getInstance();
		now.clear();
		now.setTimeZone(InmarsatDefinition.API_TIMEZONE);
		now.set(nowYear, nowMonth, nowDay, nowHour, nowMin);
		return now;
	}

	@Test
	public void equalsAndHashCode() throws InmarsatException {
		PositionDate positionDate1 = new PositionDate(23, 1, 2);
		PositionDate positionDate2 = new PositionDate(23, 1, 2);
		PositionDate positionDate3 = new PositionDate(23, 1, 3);

		assertEquals(positionDate1, positionDate2);
		assertEquals(positionDate2, positionDate1);
		assertEquals(positionDate1.hashCode(), positionDate2.hashCode());

		assertNotEquals(positionDate1, positionDate3);
		assertNotEquals(positionDate1.hashCode(), positionDate3.hashCode());

		positionDate3 = new PositionDate(23, 1, 2, new PositionDate.PositionDateExtra(
				2016 - PositionDate.PositionDateExtra.FORMAT1_YEARSTART, Calendar.DECEMBER + 1));

		assertNotEquals(positionDate1, positionDate3);
		assertNotEquals(positionDate3, positionDate1);
		assertNotEquals(positionDate1.hashCode(), positionDate3.hashCode());

		PositionDate positionDate4 = new PositionDate(23, 1, 2, new PositionDate.PositionDateExtra(
				2016 - PositionDate.PositionDateExtra.FORMAT1_YEARSTART, Calendar.DECEMBER + 1));
		assertEquals(positionDate3, positionDate4);
		assertEquals(positionDate4, positionDate3);
		assertEquals(positionDate3.hashCode(), positionDate4.hashCode());

	}

}
