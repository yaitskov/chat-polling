package dan.dao;

import dan.client.Page;
import dan.entity.MessageEnt;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * @author Daneel S. Yaitskov
 */
@Repository
public class MessageDao extends Dao<MessageEnt> {

    private static final String COUNT_QUERY = "select count(m) from Message m " +
            "where m.topic.id = :t " +
            "and m.created >= :s " +
            "and m.created <= :e";

    /**
     * order by 2 fields eliminate undetermined situation
     * if two messages have exactly the same creation time
     * then you cannot rely on the offset.
     *
     * I don't think that using unique message id is good idea
     * because it can overflow.
     */
    private static final String FIND_RANGE_QUERY = "select m from Message m " +
            "where m.topic.id = :t " +
            "and m.created >= :s " +
            "and m.created <= :e " +
            "order by m.created, m.id";

    private static final String FIND_FIRST_QUERY = "select m from Message m " +
            "where m.topic.id = :t " +
            "and m.created >= :s " +
            "order by m.created, m.id";


    private static final int PAGE_SIZE = 100;

    /**
     *
     * @param topicId
     * @param start
     * @param end
     * @param page
     * @return ordered by asc creation date
     */
    public Page<MessageEnt> findRange(int topicId, Date start, Date end, int page) {
        Query q = em().createQuery(FIND_RANGE_QUERY);
        q.setParameter("t", topicId);
        q.setParameter("s", start);
        q.setParameter("e", end);
        q.setFirstResult(page * PAGE_SIZE);
        q.setMaxResults(PAGE_SIZE);

        List<MessageEnt> messages = q.getResultList();
        return new Page<MessageEnt>(messages, countTotal(topicId, start, end), PAGE_SIZE);
    }

    /**
     * Counts total messages in the topic between dates inclusively.
     * @param topicId
     * @param start
     * @param end
     * @return total number messages between dates inclusively
     */
    public long countTotal(int topicId, Date start, Date end) {
        Query q = em().createQuery(COUNT_QUERY);
        q.setParameter("t", topicId);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return (Long) q.getSingleResult();
    }

    /**
     * Returns first up to PAGE-SIZE messages since the last time skipping
     * the specified number messages.
     * @param topicId where message to search
     * @param number  number messages to skip from the list head
     * @param last    date and time last message
     * @return messages ordered by asc.
     */
    public List<MessageEnt> findNearestAfter(int topicId, int number, Date last) {
        Query q = em().createQuery(FIND_FIRST_QUERY);
        q.setParameter("t", topicId);
        q.setParameter("s", last);
        q.setFirstResult(number);
        q.setMaxResults(PAGE_SIZE);

        return q.getResultList();
    }
}
