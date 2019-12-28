package net.smiguel.stellar.support.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Util {

    public static final String DATE_COUNT_DOWN = "yyyy/MM/dd";
    public static final String DATE_SIMPLE = "dd/MM/yyyy";
    public static final String DATETIME_SIMPLE = "dd/MM/yyyy HH:mm";
    public static final String DATETIME_COMPLETE = "dd/MM/yyyy HH:mm:ss";

    public static final String DATE_ISO_8601 = "yyyy-MM-dd";
    public static final String DATETIME_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIME = "HH:mm:ss";
    public static final String TIME_SIMPLE = "HH:mm";

    public static Date getDateFromStellarApi(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return getDateFormatForStellarApi().parse(value);

        } catch (ParseException e) {
            //Used only for demonstration purpose
            e.printStackTrace();
            return null;
        }
    }

    public static Date getDateIso8601(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return getDateSimpleFormat().parse(value);

        } catch (ParseException e) {
            //Used only for demonstration purpose
            e.printStackTrace();
            return null;
        }
    }

    public static String getDateFromStellarDate(Date date) {
        if (date == null) {
            return null;
        }
        return getDateFormat().format(date);
    }

    public static String getSimpleDate(Date date) {
        if (date == null) {
            return null;
        }
        return getDateSimpleFormat().format(date);
    }

    public static String getSimpleTime(Date date) {
        if (date == null) {
            return null;
        }
        return getTimeSimpleFormat().format(date);
    }

    public static String getTimeFromStellarDate(Date date) {
        if (date == null) {
            return null;
        }
        return getTimeFormat().format(date);
    }

    public static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat(DATE_ISO_8601);
    }

    public static SimpleDateFormat getDateSimpleFormat() {
        return new SimpleDateFormat(DATE_SIMPLE);
    }

    public static SimpleDateFormat getDatetimeFormat() {
        return new SimpleDateFormat(DATETIME_SIMPLE);
    }

    public static SimpleDateFormat getCompleteDatetimeFormat() {
        return new SimpleDateFormat(DATETIME_COMPLETE);
    }

    public static SimpleDateFormat getTimeFormat() {
        return new SimpleDateFormat(TIME);
    }

    public static SimpleDateFormat getTimeSimpleFormat() {
        return new SimpleDateFormat(TIME_SIMPLE);
    }

    public static SimpleDateFormat getDateFormatForStellarApi() {
        return new SimpleDateFormat(DATETIME_ISO_8601);
    }

    public static String formatDateSimple(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return getDateSimpleFormat().format(calendar.getTime());
    }

    public static String formatDatetime(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return getDatetimeFormat().format(calendar.getTime());
    }

    public static String formatCurrentDatetime() {
        return getCompleteDatetimeFormat().format(getCurrentDate().getTime());
    }

    public static SimpleDateFormat getDateFormatIso8601() {
        SimpleDateFormat SDF_ISO_8601_DATE = new SimpleDateFormat(DATE_ISO_8601);
        SDF_ISO_8601_DATE.setLenient(false);
        return SDF_ISO_8601_DATE;
    }

    public static SimpleDateFormat getDateTimeFormatIso8601() {
        SimpleDateFormat SDF_ISO_8601_DATETIME = new SimpleDateFormat(DATETIME_ISO_8601);
        SDF_ISO_8601_DATETIME.setLenient(false);
        return SDF_ISO_8601_DATETIME;
    }

    public static Calendar getCurrentDate() {
        //Not used a spec locale. only for demonstration purpose
        return new GregorianCalendar();
    }

    public static Calendar getDate(long date) {
        Calendar setDate = getCurrentDate();
        setDate.setTime(new Date(date));
        return setDate;
    }

    public static Calendar toCalendar(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = getCurrentDate();
        calendar.setTime(date);
        return calendar;
    }
}
