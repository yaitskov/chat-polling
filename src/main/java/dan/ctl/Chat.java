package dan.ctl;

import dan.client.MessageWeb;
import dan.client.Page;
import dan.dao.MessageDao;
import dan.dao.TopicDao;
import dan.entity.MessageEnt;
import dan.entity.Topic;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Daneel S. Yaitskov
 */
@Controller
public class Chat {

    private static final Logger logger = LoggerFactory.getLogger(Chat.class);

    @Resource
    private MessageDao messageDao;

    @Resource
    private TopicDao topicDao;
    
    @Resource(name = "ttReuse")
    private TransactionTemplate ttReuse;

    @RequestMapping("/chat")
    public String index() {
        return "index";
    }

    @Transactional(readOnly = true)
    @ResponseBody
    @RequestMapping("/history")
    public Page<MessageWeb> history(
            @RequestParam("topic") int topicId,
            @RequestParam(value = "start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(value = "end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end,
            @RequestParam(value ="page", defaultValue = "0") int page)
    {
        if (end == null) {
            throw new ValidationException("end is null");
        }
        if (start == null) {
            throw new ValidationException("start is null");
        }
        if (end.before(start)) {
            throw new ValidationException("start date is greater than end date");
        }
        Page<MessageEnt> dbMessages = messageDao.findRange(topicId, start, end, page);
        List<MessageWeb> webMessages = MessageWeb.fromEntities(dbMessages.getItems());
        Page<MessageWeb> result = new Page<MessageWeb>(webMessages, dbMessages.getPages());
        return result;
    }


    @RequestMapping(value = "/send")
    @ResponseBody
    public int sendMessage(@RequestParam("topic") int topicId,
                           @RequestParam("content") String content,
                           @RequestParam(value = "author", defaultValue = "") String author)
    {
        MessageEnt message = persistMessage(topicId, content, author);
        Broadcaster bc = BroadcasterFactory.getDefault().lookup(topicId, true);
        bc.broadcast("value is no matter. just signal to wake up.");
        return message.getId();
    }

    /**
     * Separate function to flush session before broadcasting.
     */
    @Transactional
    private MessageEnt persistMessage(int topicId, String content, String author) {
        logger.info("send message topic = {}, content = {}, author = {}",
                new Object[]{ topicId, content, author});
        Topic topic = topicDao.find(topicId);
        MessageEnt message = new MessageEnt();
        message.setAuthor(author);
        message.setCreated(new Date());
        message.setBody(content);
        message.setTopic(topic);
        messageDao.save(message);
        return message;
    }

    @PostConstruct    
    private void buildTopic() {
        ttReuse.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Topic topic = new Topic();
                topic.setTitle("default");
                topicDao.save(topic);

                logger.info("default topic was created");
            }
        });
    }
}
