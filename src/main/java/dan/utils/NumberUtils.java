package dan.utils;

import com.google.common.base.Function;

/**
 * @author Daneel S. Yaitskov
 */
public class NumberUtils {
    public static final Function<String, Integer> parseIntF
            = new Function<String, Integer>() {
        @Override
        public Integer apply(String input) {
            return Integer.parseInt(input);
        }
    };
}
