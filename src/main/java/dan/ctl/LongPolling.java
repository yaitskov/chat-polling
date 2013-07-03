package dan.ctl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dan.client.ErrorResponse;
import dan.client.MessageWeb;
import dan.dao.MessageDao;
import dan.entity.MessageEnt;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Daneel S. Yaitskov
 */
@AtmosphereHandlerService(path = "/api/get")
@Configurable  // class instantiated by atmosphere so  magic is required
public class LongPolling implements AtmosphereHandler {

    private final Logger logger = LoggerFactory.getLogger(LongPolling.class);

    @Resource
    private MessageDao messageDao;

    @Resource(name = "om")
    protected ObjectMapper mapper;

    @PostConstruct
    public void init() {
        logger.info("Initializing LongPolling");
    }


    private static class Update {
        public int topicId;
        /**
         * number of last messages with the same post time on the client
         */
        public int number;
        /**
         * date and time of last message
         */
        public Date last;
    }

    protected Update createFromRequest(AtmosphereRequest request) {
        Update result = new Update();
        String topicS = request.getParameter("topic");
        if (topicS == null) {
            throw new IllegalArgumentException("topic parameter is required");
        } else {
            try {
                result.topicId = Integer.parseInt(topicS);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'topic' parameter is not number");
            }
        }
        String numberS =  request.getParameter("number");
        if (numberS != null) {
            try {
            result.number = Integer.parseInt(numberS);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'number' parameter is not number");
            }
        }
        String lastS = request.getParameter("last");
        if (lastS != null) {
            try {
                result.last = new Date(Long.parseLong(lastS));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'last' parameter invalid");
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MessageWeb> selectNearestAfter(Update parameters) {
        List<MessageEnt> dbMessages;
        if (parameters.last == null) {
            dbMessages = messageDao.findLastMessages(parameters.topicId);
        } else {
            dbMessages = messageDao.findNearestAfter(parameters.topicId,
                    parameters.number, parameters.last);
        }
        List<MessageWeb> result = MessageWeb.fromEntities(dbMessages);
        return result;
    }

    protected void setTopicBroadcaster(AtmosphereResource resource, int topicId) {
        Broadcaster dbc = resource.getBroadcaster();
        dbc.removeAtmosphereResource(resource);
        Broadcaster bc = BroadcasterFactory.getDefault().lookup(topicId, true);
        resource.setBroadcaster(bc);
        bc.addAtmosphereResource(resource);
    }

    /**
     * New Ajax request arrived.
     */
    @Override
    public void onRequest(AtmosphereResource resource)
            throws IOException
    {
        try {
            // encoding http filter ignored in atmosphere servlet
            // resource.getResponse().setContentType("application/json; charset=UTF-8");
            logger.info("on request");
             AtmosphereRequest request = resource.getRequest();
            Update parameters = createFromRequest(request);
            setTopicBroadcaster(resource, parameters.topicId);
            List<MessageWeb> result = selectNearestAfter(parameters);
            if (result.isEmpty()) {
                // until some body write a message or timeout
                resource.suspend();
            } else {
                writeResponse(resource, result);
            }
        } catch (IllegalArgumentException e) {
            writeResponse(resource, new ErrorResponse(e));
        }
    }

    private void writeResponse(AtmosphereResource resource, Object value)
            throws IOException
    {
        mapper.writeValue(resource.getResponse().getWriter(), value);
    }

    /**
     * Some body have written a message or timeout.
     */
    @Override
    public void onStateChange(AtmosphereResourceEvent event)
            throws IOException
    {
        logger.info("on state change");
        AtmosphereResource resource = event.getResource();

        if (event.isSuspended()) {
            Update parameters = createFromRequest(resource.getRequest());
            List<MessageWeb> result = selectNearestAfter(parameters);
            writeResponse(resource, result);
            resource.resume();
        } else if (event.isCancelled() || event.isResumedOnTimeout()) {
            writeResponse(resource, new ArrayList());
        }
    }

    @Override
    public void destroy() {
        logger.info("destroy atmo handler");

    }
}