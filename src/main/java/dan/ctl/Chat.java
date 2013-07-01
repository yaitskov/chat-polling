package dan.ctl;

import dan.client.MessageWeb;
import dan.client.Ok;
import dan.client.Page;
import dan.dao.MessageDao;
import dan.dao.TopicDao;
import dan.entity.MessageEnt;
import dan.entity.Topic;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Daneel S. Yaitskov
 */
@Controller
public class Chat {

    @Resource
    private MessageDao messageDao;

    @Resource
    private TopicDao topicDao;

    @RequestMapping("/chat")
    public String index() {
        return "index";
    }

    @Transactional(readOnly = true)
    @ResponseBody
    @RequestMapping("/history")
    public Page<MessageWeb> history(
            @RequestParam("topic") int topicId,
            @RequestParam("start") Date start,
            @RequestParam("end") Date end,
            @RequestParam(value ="page", defaultValue = "0") int page)
    {
        Page<MessageEnt> dbMessages = messageDao.findRange(topicId, start, end, page);
        List<MessageWeb> webMessages = convertMessages(dbMessages.getItems());
        Page<MessageWeb> result = new Page<MessageWeb>(webMessages, dbMessages.getPages());
        return result;
    }

    private List<MessageWeb> convertMessages(List<MessageEnt> dbMessages) {
        List<MessageWeb> webMessages = new ArrayList<MessageWeb>(dbMessages.size());
        for (MessageEnt ent : dbMessages) {
            webMessages.add(new MessageWeb(ent.getBody(), ent.getCreated(), ent.getAuthor()));
        }
        return webMessages;
    }

    @Transactional()
    @ResponseBody
    @RequestMapping("/send")
    public Ok sendMessage(@RequestParam("topic") int topicId,
                          @RequestParam("content") String content,
                          @RequestParam(value = "author", defaultValue = "") String author)
    {
        Topic topic = topicDao.find(topicId);
        MessageEnt message = new MessageEnt();
        message.setAuthor(author);
        message.setCreated(new Date());
        message.setBody(content);
        message.setTopic(topic);
        messageDao.save(message);
        return Ok.OK;
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
            @RequestParam("number") int number,
            @RequestParam("last") Date last)
    {
        List<MessageEnt> dbMessages = messageDao.findNearestAfter(topicId, number, last);
        List<MessageWeb> result = convertMessages(dbMessages);
        return result;
    }

    @PostConstruct
    @Transactional
    private void buildTopic() {
        Topic topic = new Topic();
        topic.setTitle("default");
        topicDao.save(topic);
    }
}
