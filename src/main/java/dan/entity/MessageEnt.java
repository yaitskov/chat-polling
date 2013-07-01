package dan.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Daneel S. Yaitskov
 */
@Entity
public class MessageEnt {

    @Id
    @GeneratedValue
    private int id;

    @Column
    private String body;

    @Column
    private String author;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @ManyToOne
    private Topic topic;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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
