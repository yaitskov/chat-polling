package dan.ctl;

import org.apache.catalina.CometEvent;
import org.apache.catalina.CometFilter;
import org.apache.catalina.CometFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

/**
 * I've found that tomcat just ignore usual http filters
 * so standard CharacterEncodingFilter has nosense.
 * To filter Long Polling request filter
 * should implement CometFilter interface.
 *
 * @author Daneel S. Yaitskov
 */
public class CometCharacterEncodingFilter implements CometFilter {

    private static final Logger logger = LoggerFactory.getLogger(CometCharacterEncodingFilter.class);

    private String encoding = "UTF-8";

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void doFilterEvent(CometEvent event, CometFilterChain chain)
            throws IOException, ServletException {
        event.getHttpServletRequest().setCharacterEncoding(encoding);
        event.getHttpServletResponse().setCharacterEncoding(encoding);
        chain.doFilterEvent(event);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        logger.info("non comment doFilter");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("destroy");
    }
}
