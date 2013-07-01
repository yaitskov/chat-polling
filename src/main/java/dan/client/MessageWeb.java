package dan.client;

import java.util.Date;

/**
 * @author Daneel S. Yaitskov
 */
public class MessageWeb {
    public String content;
    public Date create;
    public String author;

    public MessageWeb(String content, Date create, String author) {
        this.content = content;
        this.create = create;
        this.author = author;
    }
}
