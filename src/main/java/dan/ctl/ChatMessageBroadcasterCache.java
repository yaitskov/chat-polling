package dan.ctl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import dan.client.MessageWeb;
import dan.dao.MessageDao;
import dan.entity.MessageEnt;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

import static org.atmosphere.cpr.HeaderConfig.X_CACHE_DATE;

/**
 * @author Daneel S. Yaitskov
 */
@Configurable
public class ChatMessageBroadcasterCache implements BroadcasterCache {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageBroadcasterCache.class);

    protected final List<CachedMessage> queue = new CopyOnWriteArrayList<CachedMessage>();

    @Resource(name = "std-executor")
    protected ScheduledExecutorService reaper;

    @Value("${cache-item-live}")
    protected int maxCachedinMs = 1000 * 5 * 60;

    @Resource
    private MessageDao messageDao;

    @Resource(name = "om")
    private ObjectMapper mapper;

    private boolean initialized;
    private Future oldMsgCleanerTask;

    public List<Object> retrieveLastMessage(String id, AtmosphereResource ar) {
        AtmosphereResourceImpl r = AtmosphereResourceImpl.class.cast(ar);
        List<Object> result = Lists.newArrayList();
        if (!r.isInScope()) return result;

        AtmosphereRequest request = r.getRequest();
        String dateString = request.getParameter(X_CACHE_DATE);
        if (dateString == null) return null;
        tryToInit(id, ar);

        long currentTime = Long.valueOf(dateString);
        for (CachedMessage cm : queue) {
            logger.info("cmp time {} > {}", cm.currentTime(), currentTime);
            if (cm.currentTime() > currentTime) {
                result.add(cm.message());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public final void start() {
        oldMsgCleanerTask = reaper.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Iterator<CachedMessage> i = queue.iterator();
                CachedMessage message;
                while (i.hasNext()) {
                    message = i.next();
                    logger.trace("Message: {}", message.message());

                    if (System.currentTimeMillis() - message.currentTime() > maxCachedinMs) {
                        logger.trace("Pruning: {}", message.message());
                        queue.remove(message);
                    } else {
                        break;
                    }
                }
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    /**
     * {@inheritDoc}
     */
    public final void stop() {
        oldMsgCleanerTask.cancel(true);
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void addToCache(String id, final AtmosphereResource resource, final Object object) {
        try {
            logger.info("Adding message for resource: {}, object: {}", resource, mapper.writeValueAsString(object));
        } catch (IOException e) {
            logger.error("{}", e);
        }

        CachedMessage cm = new CachedMessage(object, ((MessageWeb)object).created.getTime());
        try {
            logger.info("before add: {}", mapper.writeValueAsString(queue));
        } catch (IOException e) {
            logger.error("{}", e);
        }
        queue.add(cm);
        try {
            logger.info("after add: {}", mapper.writeValueAsString(queue));
        } catch (IOException e) {
            logger.error("{}", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized List<Object> retrieveFromCache(String id, AtmosphereResource r) {
        List result = retrieveLastMessage(id, r);
        try {
            logger.info("objectts from cache: {}", mapper.writeValueAsString(result));
        } catch (IOException e) {
            logger.error("{}", e);
        }
        return result;
    }

    /**
     * Get the maximum time a broadcasted message can stay cached.
     *
     * @return Get the maximum time a broadcasted message can stay cached.
     */
    public int getMaxCachedinMs() {
        return maxCachedinMs;
    }

    /**
     * Set the maximum time a broadcasted message can stay cached.
     *
     * @param maxCachedinMs time in milliseconds
     */
    public void setMaxCachedinMs(final int maxCachedinMs) {
        this.maxCachedinMs = maxCachedinMs;
    }

    protected final static class CachedMessage implements Serializable {

        public final Object message;
        public final long currentTime;

        public CachedMessage() {
            this.currentTime = System.currentTimeMillis();
            this.message = null;
        }

        public CachedMessage(Object message, long currentTime) {
            this.currentTime = currentTime;
            this.message = message;
        }

        public Object message() {
            return message;
        }

        public long currentTime() {
            return currentTime;
        }

        public String toString() {
            if (message != null) {
                return message.toString();
            } else {
                return "";
            }
        }
    }

    private void loadInitCache(String id, AtmosphereResource ar) {
        try {
            List<MessageEnt> last = messageDao.findLastMessages(Integer.parseInt(id));
            for (MessageEnt messageEnt : last) {
                addToCache(id, ar, MessageWeb.fromEntity(messageEnt));
            }
        } catch (NumberFormatException e) {
            logger.error("failed load cache for broadcaster '{}' {}", id, e);
        }
    }

    private void tryToInit(String id, AtmosphereResource ar) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    logger.info("preload cache");
                    initialized = true;
                    loadInitCache(id, ar);
                }
            }
        }
    }
}
