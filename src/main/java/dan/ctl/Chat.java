package dan.ctl;

import dan.client.MessageWeb;
import dan.client.Page;
import dan.dao.MessageDao;
import dan.dao.TopicDao;
import dan.entity.MessageEnt;
import dan.entity.Topic;
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
        List<MessageWeb> webMessages = convertMessages(dbMessages.getItems());
        Page<MessageWeb> result = new Page<MessageWeb>(webMessages, dbMessages.getPages());
        return result;
    }

    @Transactional
    @RequestMapping(value = "/send")
    @ResponseBody
    public int sendMessage(@RequestParam("topic") int topicId,
                          @RequestParam("content") String content,
                          @RequestParam(value = "author", defaultValue = "") String author)
    {
        logger.info("send message topic = {}, content = {}, author = {}",
                new Object[]{ topicId, content, author});
        Topic topic = topicDao.find(topicId);
        MessageEnt message = new MessageEnt();
        message.setAuthor(author);
        message.setCreated(new Date());
        message.setBody(content);
        message.setTopic(topic);
        messageDao.save(message);
        return message.getId();
    }

    /**
     *
     * @param topicId
     * @param number    number of last messages with the same post time on the client
     * @param last      date and time of last message
     * @return
     */
    @Transactional(readOnly = true)
    @RequestMapping("/get")
    @ResponseBody
    public List<MessageWeb> selectNearestAfter(
            @RequestParam("topic") int topicId,
            @RequestParam(value = "number", defaultValue = "0") int number,
            @RequestParam(value = "last", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date last)
    {
        logger.info("get messages topic = {}, number = {}, last = {}",
                new Object[]{ topicId, number, last});
        List<MessageEnt> dbMessages;
        if (last == null) {
            dbMessages = messageDao.findLastMessages(topicId);
        } else {
            dbMessages = messageDao.findNearestAfter(topicId, number, last);
        }
        List<MessageWeb> result = convertMessages(dbMessages);
        return result;
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

    private List<MessageWeb> convertMessages(List<MessageEnt> dbMessages) {
        List<MessageWeb> webMessages = new ArrayList<MessageWeb>(dbMessages.size());
        for (MessageEnt ent : dbMessages) {
            webMessages.add(new MessageWeb(ent.getBody(), ent.getCreated(), ent.getAuthor()));
        }
        return webMessages;
    }
}
