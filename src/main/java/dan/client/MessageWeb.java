package dan.client;

import java.util.Date;

/**
 * @author Daneel S. Yaitskov
 */
public class MessageWeb {
    public String content;
    public Date created;
    public String author;

    public MessageWeb(String content, Date created, String author) {
        this.content = content;
        this.created = created;
        this.author = author;
    }
}
