package dan.exception;

import dan.utils.ClassMap;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.Constructor;

/**
 * Constrains value to be class and class has public constructor
 * that takes 1 argument exception it can handle.
 *
 * Daneel Yaitskov
 */
public class ValueClassTakesException extends KeyExceptionClassMap<Class> {

    public ValueClassTakesException() {
    }

    public ValueClassTakesException(ClassMap source) {
        super(source);
    }

    /**
     *
     * @param exception exception class
     * @param response response class
     * @return
     */
    @Override
    public Class put(Class exception, Class response) {
        Constructor c = ConstructorUtils.getMatchingAccessibleConstructor(
                response,
                exception);
        if (c == null)
            throw new IllegalArgumentException("value class "
            + response + " doesn't have constructor that accepts "
            + " object of class " + exception);
        return super.put(exception, response);
    }
}
