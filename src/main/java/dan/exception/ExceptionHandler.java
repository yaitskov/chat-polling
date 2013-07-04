package dan.exception;

import dan.utils.LogUtil;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Maps exception that can be thrown in any controller and
 * converts it into POJO object with converter bean
 * or response object whose constructor takes 1 argument of
 * compatible type.
 * <p/>
 * Response object is converted into JSON with Jackson library.
 * <p/>
 * I've found interesting thing. HandlerExceptionResolver method
 * takes only exception that is subclass of java.lang.Exception.
 * <p/>
 * What about Throwable? In case of Throwable HandlerExceptionResolver
 * gets UndeclaredThrowableException that refers to my exception object.
 * <p/>
 * Daneel Yaitskov
 */
public class ExceptionHandler implements HandlerExceptionResolver, Ordered {

    private static final Logger logger = LogUtil.get();

    /**
     * Render error response object.
     */
    private JsonView jview;

    /**
     * Exception class => converter.
     */
    private KeyExceptionClassMap<ExceptionConverter> exceptionConverter;

    /**
     * Exception class => response class
     * that have constructor taking exception object
     */
    private ValueClassTakesException exceptionResponse;

    public KeyExceptionClassMap<ExceptionConverter> getExceptionConverter() {
        return exceptionConverter;
    }

    /**
     * @param exceptionConverter
     */
    public void setExceptionConverter(
            KeyExceptionClassMap<ExceptionConverter> exceptionConverter) {
        this.exceptionConverter = exceptionConverter;
    }

    public ValueClassTakesException getExceptionResponse() {
        return exceptionResponse;
    }

    public void setExceptionResponse(ValueClassTakesException exceptionResponse) {
        this.exceptionResponse = exceptionResponse;
    }

    public JsonView getJview() {
        return jview;
    }

    public void setJview(JsonView jview) {
        this.jview = jview;
    }

    @PostConstruct
    public void init() {
        if (exceptionConverter == null) {
            exceptionConverter = new KeyExceptionClassMap<ExceptionConverter>();
        }
        logger.debug("add default UndeclaredThrowableException converter");
        exceptionConverter.put(
                UndeclaredThrowableException.class,
                new UndeclaredThrowableExceptionConverter());
    }

    /**
     * Returns JSON view with bounded converted response object
     * or null if resolver was not able to convert exception.
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @return
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        try {
            Object responseObj = convertToResponse(ex);
            if (responseObj == null) {
                return null;
            }
            ModelAndView mav = new ModelAndView();
            mav.setView(jview);
            mav.addObject(JsonView.DATA_FIELD_NAME, responseObj);
            return mav;
        } catch (InstantiationException e) {
            handleInternalException(e, ex);
        } catch (IllegalAccessException e) {
            handleInternalException(e, ex);
        } catch (InvocationTargetException e) {
            handleInternalException(e, ex);
        } catch (NoSuchMethodException e) {
            handleInternalException(e, ex);
        }
        return null;
    }

    protected void handleInternalException(Exception system, Exception data) {
        logger.error("Failed convert exception: " + data, system);
    }

    /**
     * @param ex
     * @return
     * @throws java.lang.reflect.InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public Object convertToResponse(Exception ex)
            throws InvocationTargetException, InstantiationException,
            IllegalAccessException, NoSuchMethodException {
        Object result = convertWithConverter(ex);
        if (result != null) {
            return result;
        }
        return convertWithConstructor(ex);
    }

    /**
     * Returns converted object or null.
     *
     * @param ex exception object to be converted into response one
     * @return converted object for serialization or null otherwise (no match)
     */
    public Object convertWithConverter(Exception ex) {
        ExceptionConverter converter = exceptionConverter.get(ex.getClass());
        if (converter != null) {
            return converter.convert(ex);
        }
        return null;
    }

    /**
     * Response class should have public constructor with 1 argument.
     *
     * @param ex exception object to be converted into response one
     * @return object for json serialization with Jackson or null if nothing matched
     * @throws java.lang.reflect.InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public Object convertWithConstructor(Exception ex)
            throws InvocationTargetException, IllegalAccessException,
            InstantiationException, NoSuchMethodException {
        Class response = exceptionResponse.get(ex.getClass());
        if (response != null) {
            logger.error("exception handled with constructor of " + response,
                    ex);
            return ConstructorUtils.invokeConstructor(response, ex);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE; // first in line
    }
}
