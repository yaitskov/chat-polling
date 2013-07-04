package dan.utils;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;

/**
 * Due specific cache implementation not all object can be
 * broadcasted now.
 * @author Daneel S. Yaitskov
 */
public class ChatMsgBroadcaster {

    private Broadcaster broadcaster;

    protected ChatMsgBroadcaster(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public static ChatMsgBroadcaster findOrCreate(Object channelName) {
        Broadcaster bc = BroadcasterFactory.getDefault().lookup(channelName, true);
        return new ChatMsgBroadcaster(bc);
    }

    public void broadcast(Dated object) {
        broadcaster.broadcast(object);
    }
}
