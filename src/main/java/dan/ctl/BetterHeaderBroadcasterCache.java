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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.atmosphere.cpr.HeaderConfig.X_CACHE_DATE;

/**
 * @author Daneel S. Yaitskov
 */
@Configurable
public class BetterHeaderBroadcasterCache implements BroadcasterCache {

    private static final Logger logger = LoggerFactory.getLogger(BetterHeaderBroadcasterCache.class);

    protected final List<CachedMessage> queue = new CopyOnWriteArrayList<CachedMessage>();

    protected ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor();

    protected int maxCachedinMs = 1000 * 5 * 60;

    @Value("${cache-item-live}")
    private int cacheItemLive;

    @Resource
    private MessageDao messageDao;

    @Resource(name = "om")
    private ObjectMapper mapper;

    private boolean initialized;

    @PostConstruct
    private void init() {
        maxCachedinMs = cacheItemLive;
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



    public List<Object> retrieveLastMessage(String id, AtmosphereResource ar) {
        AtmosphereResourceImpl r = AtmosphereResourceImpl.class.cast(ar);
        List<Object> result = Lists.newArrayList();
        if (!r.isInScope()) return result;

        AtmosphereRequest request = r.getRequest();
        String dateString = request.getHeader(X_CACHE_DATE);
        if (dateString == null) return null;
        tryToInit(id, ar);

        long currentTime = Long.valueOf(dateString);
        CachedMessage last = null;
        for (CachedMessage cm : queue) {
            logger.info("cmp time {} > {}", cm.currentTime(), currentTime);
            if (cm.currentTime() > currentTime) {
                result.add(cm.message());
                last = cm;
            }
        }
        if (!result.isEmpty()) {
            cache(id, ar, last);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public final void start() {
        reaper.scheduleAtFixedRate(new Runnable() {

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

    public void setExecutorService(ScheduledExecutorService reaper){
        if (reaper != null) {
            stop();
        }
        this.reaper = reaper;
    }

    /**
     * {@inheritDoc}
     */
    public final void stop() {
        reaper.shutdown();
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

        CachedMessage cm = new CachedMessage(object, System.currentTimeMillis(), null);
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

    public void cache(String id, AtmosphereResource ar, CachedMessage cm) {
        long time = cm.currentTime();

        AtmosphereResourceImpl r = AtmosphereResourceImpl.class.cast(ar);
        if (r != null && r.isInScope() && !r.getResponse().isCommitted()) {
            logger.info("set x-cache-date to {}", time);
            r.getResponse().addHeader(X_CACHE_DATE, String.valueOf(time));
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
        public CachedMessage next;
        public final boolean isTail;
        public Object t;

        public CachedMessage(boolean isTail) {
            this.currentTime = System.currentTimeMillis();
            this.message = null;
            this.next = null;
            this.isTail = isTail;
        }

        public CachedMessage(Object message, long currentTime, CachedMessage next) {
            this.currentTime = currentTime;
            this.message = message;
            this.next = next;
            this.isTail = false;
        }

        public Object message() {
            return message;
        }

        public long currentTime() {
            return currentTime;
        }

        public CachedMessage next() {
            return next;
        }

        public CachedMessage next(CachedMessage next) {
            this.next = next;
            return next;
        }

        public String toString() {
            if (message != null) {
                return message.toString();
            } else {
                return "";
            }
        }

        public boolean isTail() {
            return isTail;
        }

        public CachedMessage setKey(Object t) {
            this.t = t;
            return this;
        }

        public Object getKey() {
            return t;
        }

    }
}