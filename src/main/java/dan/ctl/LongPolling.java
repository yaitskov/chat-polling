package dan.ctl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private static class NewMessageReqParams {
        public int topicId;
    }

    protected NewMessageReqParams createFromRequest(AtmosphereRequest request) {
        NewMessageReqParams result = new NewMessageReqParams();
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
            logger.info("on request");
            AtmosphereRequest request = resource.getRequest();
            NewMessageReqParams parameters = createFromRequest(request);
            setTopicBroadcaster(resource, parameters.topicId);
            // rely on cache. if it has undelivered messages then
            // this request will be resumed.
            resource.suspend(30000, false);
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
            throws IOException {
        logger.info("on state change");
        AtmosphereResource resource = event.getResource();

        if (event.isCancelled() || event.isResumedOnTimeout()) {
            writeResponse(resource, new ArrayList());
        } else {
            if (event.getMessage() instanceof List) {
                writeResponse(resource, event.getMessage());
            } else {
                writeResponse(resource, Lists.newArrayList(event.getMessage()));
            }
            resource.resume();
        }
    }

    @Override
    public void destroy() {
        logger.info("destroy atmo handler");
    }
}