package dan.client;

import com.google.common.base.Function;
import dan.entity.MessageEnt;
import dan.utils.DatedIdentified;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Daneel S. Yaitskov
 */
public class MessageWeb implements DatedIdentified {
    public int id;
    public String content;
    public Date created;
    public String author;

    public MessageWeb(int id, String content, Date created, String author) {
        this.id = id;
        this.content = content;
        this.created = created;
        this.author = author;
    }

    public static MessageWeb fromEntity(MessageEnt ent) {
        return new MessageWeb(ent.getId(), ent.getBody(), ent.getCreated(), ent.getAuthor());
    }

    public static Function<MessageEnt, MessageWeb> fromEntityF() {
        return new Function<MessageEnt, MessageWeb>() {
            @Override
            public MessageWeb apply(MessageEnt input) {
                return fromEntity(input);
            }
        };
    }

    public static List<MessageWeb> fromEntities(List<MessageEnt> dbMessages) {
        List<MessageWeb> webMessages = new ArrayList<MessageWeb>(dbMessages.size());
        for (MessageEnt ent : dbMessages) {
            webMessages.add(fromEntity(ent));
        }
        return webMessages;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public int getId() {
        return id;
    }
}
