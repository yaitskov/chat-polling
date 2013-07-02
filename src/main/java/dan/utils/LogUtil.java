/**
 * 
 */
package dan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create logger bound to the caller class.
 *
 * @author Daneel S. Yaitskov (rtfm.rtfm.rtfm@gmail.com) 2012 year
 * 
 */
public class LogUtil {
    /**
     * Should be called in static initialization block of a class
     * the logger should be bound.
     *
     * @return logger bound to the caller class
     */
	public static Logger get() {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2]; 
		return LoggerFactory.getLogger(caller.getClassName());
	}
}
