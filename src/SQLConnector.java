import java.sql.*;
import java.util.ArrayList;

public class SQLConnector {
    Connection conn;
    public static final String TABLE_DICTONARY = "UNIGRAMM";
    public static final String TABLE_CLASES = "CLASES";
    public static final String TABLE_SOURCES = "SOURCES";
    public static final String TABLE_SOURCES_IN_UNIGRAM = "SOURCES_IN_UNIGRAMM";
    public static final int COUNT_OF_CLASES = 2;

    public SQLConnector() throws SQLException {
        conn = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");

    }

    public void initSQL() throws SQLException {
        String[] sql = new String[4];
        sql[0] = "CREATE TABLE IF NOT EXISTS "+TABLE_DICTONARY+"(ID INT AUTO_INCREMENT PRIMARY KEY, VALUE varchar(255));";
        sql[1] = "CREATE TABLE IF NOT EXISTS "+TABLE_CLASES+"(ID INT AUTO_INCREMENT PRIMARY KEY, NAME varchar(255));";
        sql[2] = "CREATE TABLE IF NOT EXISTS "+TABLE_SOURCES+"(ID INT AUTO_INCREMENT PRIMARY KEY, CLASS_ID int, SAMPLE text);";
        sql[3] = "CREATE TABLE IF NOT EXISTS "+TABLE_SOURCES_IN_UNIGRAM+"(TEXT_ID int, UNIGRAMM_ID int);";

        this.execute(sql);
    }

    public void execute(String sql) throws SQLException {
        Statement st = conn.createStatement();
        st.execute(sql);
    }

    public void execute(String[] sql) throws SQLException {
        Statement st = conn.createStatement();
        for (String oneSql:sql) {
            st.execute(oneSql);
        }
    }

    public void execute(ArrayList<String> sql) throws SQLException {
        Statement st = conn.createStatement();
        for (String oneSql:sql) {
            st.execute(oneSql);
        }
    }

    public ResultSet getResult(String sql) throws SQLException {
            Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
            return st.executeQuery(sql);
    }

    public void stop() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
