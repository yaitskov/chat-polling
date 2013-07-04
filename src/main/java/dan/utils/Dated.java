package dan.utils;

import java.util.Date;

/**
 * I rewrote atmosphere cache realization to get it work.
 * This cache don't use current time so it should take time from
 * the objects.
 *
 * @author Daneel S. Yaitskov
 */
public interface Dated {
    Date getCreated();
}
