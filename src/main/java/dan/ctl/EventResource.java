package dan.ctl;

import com.sun.jersey.spi.resource.Singleton;
import org.atmosphere.annotation.Broadcast;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.*;
import org.atmosphere.jersey.Broadcastable;
import org.atmosphere.jersey.SuspendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.EventListener;

/**
 * @author Daneel S. Yaitskov
 */
@Path("/{topic}")
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {

    private final Logger logger = LoggerFactory.getLogger(EventResource.class);

    @PathParam("topic")
    private Broadcaster topic;

    @PostConstruct
    public void init() {
        logger.info("Initializing EventResource");
    }

    @GET
    @Suspend
    public Broadcastable subscribe() {
        return new Broadcastable(topic);
    }

    @POST
    @Broadcast
    public Broadcastable send(@FormParam("message") String message) {
        return new Broadcastable(new JAXBBean(message), topic);
    }
}