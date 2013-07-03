package dan.ctl;

import dan.dao.MessageDao;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ws.rs.PathParam;
import java.io.IOException;

/**
 * @author Daneel S. Yaitskov
 */
//@Path("/{topic}")
@AtmosphereHandlerService(path = "/api/hel")
@Configurable
//@Produces(MediaType.APPLICATION_JSON)
public class EventResource implements AtmosphereHandler {

    private final Logger logger = LoggerFactory.getLogger(EventResource.class);

    @PathParam("topic")
    private Broadcaster topic;

    @Resource
    private MessageDao messageDao;

    @PostConstruct
    public void init() {
        logger.info("Initializing EventResource");
    }

    @Override
    public void onRequest(AtmosphereResource r) throws IOException {
        logger.info("on requeset ");
        AtmosphereRequest req = r.getRequest();
        String c = req.getParameter("c"); //channel
        if (c != null) {
            Broadcaster dbc = r.getBroadcaster();
            dbc.removeAtmosphereResource(r);

            Broadcaster bc = BroadcasterFactory.getDefault().lookup(c, true);
            r.setBroadcaster(bc);
            bc.addAtmosphereResource(r);
        }
        String p = req.getParameter("m"); // short for message
        if (p == null) {
            r.suspend();
        } else {
            String m = req.getParameter("m");
            r.getBroadcaster().broadcast(m);
            // write answer to senders
            r.getResponse().getWriter().write("ok");
        }
    }

    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {
        logger.info("on state change ");
        AtmosphereResource r = event.getResource();
        AtmosphereResponse res = r.getResponse();

        if (event.isSuspended()) {
            String body = event.getMessage().toString();
            res.getWriter().write(body);
            switch (r.transport()) {
                case JSONP:
                case AJAX:
                case LONG_POLLING:
                    event.getResource().resume();
                    break;
                default:
                    event.getResource().resume();
                    //                    res.getWriter().flush();
                    break;
            }
        } else if (!event.isResuming()) {
            logger.info("event is not resuming");
            // event.broadcaster().broadcast(new Data("Someone", "say bye bye!").toString());
        }

    }

    @Override
    public void destroy() {
        logger.info("destroy atmo handler");

    }
}