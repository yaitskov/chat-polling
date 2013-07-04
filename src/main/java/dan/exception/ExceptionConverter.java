package dan.exception;

/**
 * Converter should log stack trace him self.
 * But Response constructor should not.
 * Daneel Yaitskov
 */
public abstract class ExceptionConverter {

    public abstract Object convert(Exception e);
}
