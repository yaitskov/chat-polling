package dan.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;
import util.BaseDao;

import javax.annotation.Resource;

/**
 * @author Daneel S. Yaitskov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:dao.xml")
public class MessageDaoTest extends BaseDao {

    @Resource
    MessageDao messageDao;

    @Resource
    TopicDao topicDao;

    @Resource(name = "ttReuse")
    TransactionTemplate ttReuse;

    @Test
    public void testSave() {

    }
}
