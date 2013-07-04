package dan.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Daneel S. Yaitskov
 */
@Entity(name = "Message")
public class MessageEnt extends AbstractEntity {

    @Column(length = 1000)
    private String body;

    @Column(length = 100)
    private String author;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @ManyToOne(optional = false)
    private Topic topic;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
