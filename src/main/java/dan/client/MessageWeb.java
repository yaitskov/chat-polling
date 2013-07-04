package dan.client;

import dan.entity.MessageEnt;
import dan.utils.Dated;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Daneel S. Yaitskov
 */
public class MessageWeb implements Dated {
    public String content;
    public Date created;
    public String author;

    public MessageWeb(String content, Date created, String author) {
        this.content = content;
        this.created = created;
        this.author = author;
    }

    public static MessageWeb fromEntity(MessageEnt ent) {
        return new MessageWeb(ent.getBody(), ent.getCreated(), ent.getAuthor());
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
}
