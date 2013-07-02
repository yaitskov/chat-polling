package util;

import org.junit.Before;

import javax.annotation.Resource;

/**
 * Daneel Yaitskov
 */
public class BaseDao {

    @Resource
    DbCleaner cleaner;

    @Before
    public void setUp() {
        cleaner.clean();
    }
}
