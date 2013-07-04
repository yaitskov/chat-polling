package util;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Purge db before each test. Get initial empty schema.
 * for hsqldb only version at least 2.2.8
 * Daneel Yaitskov
 */
public class DbCleaner {

    @Resource
    DataSource ds;

    public void clean() {
        Connection connection = null;
        try {
            connection = ds.getConnection();
            try {
                Statement stmt = connection.createStatement();
                try {
                    String[] stmts = new String[] {
                            "TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK",
                            // per row lock instead table lock by default
                            "SET DATABASE TRANSACTION CONTROL MVCC"//,
//                            "SET DATABASE DEFAULT ISOLATION LEVEL Repeatable reads",
//                            "SET DATABASE TRANSACTION ROLLBACK ON DEADLOCK TRUE"
                    };
                    for (String query : stmts) {
                        stmt.execute(query);
                    }
                    connection.commit();
//                    connection.
                } finally {
                    stmt.close();
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
