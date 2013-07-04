package dan.utils;

import java.util.Date;

/**
 * @author Daneel S. Yaitskov
 */
public class DateUtils {

    public static Date shiftHours(Date d, long hours) {
        return new Date(d.getTime() + hours * 3600L * 1000L);
    }
}
