package dan.ctl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.*;
import dan.client.MessageWeb;
import dan.dao.MessageDao;
import dan.utils.DatedIdentified;
import dan.utils.NumberUtils;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.BroadcasterCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cache using requires complex algorithm because
 * I though that messages with earlier creation date might be
 * inserted later.
 * Also there are probability to have messages with the same creation time.
 * Primary key can overflow.
 *
 * I.e. using single barrier date to distinguish
 * which messages are already sent to specific client
 * and which are not is not applicable.
 *
 * There is a dangerous to lose or to duplicate messages.
 * I decided to pass other date from nearest past where
 * I can be sure all messages with creation date less or equal
 * are delivered and they cannot show up in the cache now.
 * Let's say client gathered ids of last 20 or more messages.
 * Creation date of message right after last in the group
 * cannot not be the same.
 *
 * The cache subtracts client's message id set from
 * subset of message ids created since the specified date.
 *
 * The result of subtraction is ids of undelivered messages.
 *
 * Further I found that if made cache size similar to client group
 * then date is not need at all.
 *
 * @author Daneel S. Yaitskov
 */
@Configurable
public class ChatMessageBroadcasterCache implements BroadcasterCache {

    public static final int CACHE_SIZE = 20;

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageBroadcasterCache.class);

    private volatile Future oldMsgCleanerTask;

    protected final List<DatedIdentified> cache = new CopyOnWriteArrayList<DatedIdentified>();

    @Resource(name = "std-executor")
    protected ScheduledExecutorService reaper;

    @Value("${cache-item-live}")
    protected volatile int maxCachedinMs = 1000 * 5 * 60;

    @Resource
    private MessageDao messageDao;

    @Resource(name = "om")
    private ObjectMapper mapper;

    private Set<Integer> lastClientMessages(AtmosphereRequest request) {
        String knownIds = request.getParameter("KnownIds");
        if (knownIds == null || knownIds.trim().isEmpty()) {
            return ImmutableSet.of();
        }
        String[] ids = knownIds.split(" ", CACHE_SIZE);
        return ImmutableSet.copyOf(
                Iterables.transform(ImmutableList.copyOf(ids),
                        NumberUtils.parseIntF));
    }

    public List<DatedIdentified> retrieveLastMessage(String topicId, AtmosphereResource ar) {
        AtmosphereResourceImpl r = (AtmosphereResourceImpl)(ar);
        if (!r.isInScope()) return ImmutableList.of();
        AtmosphereRequest request = r.getRequest();
        Set<Integer> lastClientMessages = lastClientMessages(request);
        if (lastClientMessages.isEmpty()) {
            return loadInitCache(topicId);
        }
        List<DatedIdentified> result = Lists.newArrayList();
        for (DatedIdentified item : cache) {
            if (!lastClientMessages.contains(item.getId())) {
                result.add(item);
            }
        }
        return result;
    }

    public final void start() {
        oldMsgCleanerTask = reaper.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Iterator<DatedIdentified> i = cache.iterator();
                DatedIdentified message;
                long current = System.currentTimeMillis();
                while (i.hasNext()) {
                    message = i.next();
                    if (current - message.getCreated().getTime() > maxCachedinMs) {
                        logger.trace("Pruning: {}", message.getId());
                        i.remove();
                    }
                }
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    public final void stop() {
        oldMsgCleanerTask.cancel(true);
    }

    public final void addToCache(String topicId, final AtmosphereResource resource, final Object object) {
        cache.add((DatedIdentified)object);
    }

    public final List<Object> retrieveFromCache(String topicId, AtmosphereResource r) {
        return (List)retrieveLastMessage(topicId, r);
    }

    private List<DatedIdentified> loadInitCache(String topicId) {
        try {
            return (List)Lists.transform(
                    messageDao.findLastMessages(Integer.parseInt(topicId)),
                    MessageWeb.fromEntityF());
        } catch (NumberFormatException e) {
            logger.error("failed load cache for broadcaster '{}' {}", topicId, e);
            throw e;
        }
    }
}
