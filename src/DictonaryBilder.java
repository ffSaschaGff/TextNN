import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

class DictonaryBilder {

    private SQLConnector sqlConnector;
    public static final int COUNT_WORD_TO_ADD = 1;

    DictonaryBilder(SQLConnector sqlConnector) {
        this.sqlConnector = sqlConnector;
    }

    void rebildDictonary() throws SQLException {
        clearExistingData();
        createDictonary();
    }

    private void clearExistingData() throws SQLException {
        String[] sql = new String[2];
        sql[0] = "delete from "+SQLConnector.TABLE_DICTONARY;
        sql[1] = "delete from "+SQLConnector.TABLE_SOURCES_IN_UNIGRAM;
        sqlConnector.execute(sql);
    }

    private void createDictonary() throws SQLException {
        ResultSet resultSet = sqlConnector.getResult("select SAMPLE, ID from " + SQLConnector.TABLE_SOURCES);

        //заполняем словарь
        ArrayList<String> input = new ArrayList<>();
        while (resultSet.next()) {
            input.add(resultSet.getString("SAMPLE"));
        }
        Unigramm unigramm = new Unigramm();
        Set<String> dictonary = unigramm.getNGram(input);
        ArrayList<String> sql = new ArrayList<>();
        int i = 0;

        for(String word: dictonary) {
            StringBuilder stringBuilder = new StringBuilder();
            sql.add(stringBuilder.append("insert into ").append(SQLConnector.TABLE_DICTONARY).append(" values (").append(String.valueOf(i+1)).append(",'").append(word).append("')").toString());

            i++;
        }

        sqlConnector.execute(sql);

        //Заполняем тексты в униграммах
        sql = new ArrayList<>();
        resultSet.beforeFirst();
        while (resultSet.next()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("INSERT INTO ").append(SQLConnector.TABLE_SOURCES_IN_UNIGRAM).append(" SELECT ").append(resultSet.getInt("ID")).append(" as TEXT_ID , ID as UNIGRAMM_ID FROM ").append(SQLConnector.TABLE_DICTONARY).append(" where VALUE in (");

            boolean isFirst = true;
            Set<String> currentWords = unigramm.getNGram(resultSet.getString("SAMPLE"));
            for (String word: currentWords) {
                if (isFirst) {
                    isFirst = !isFirst;
                } else {
                    stringBuilder.append(",");
                }
                stringBuilder.append("'").append(word).append("'");
            }
            stringBuilder.append(")");
            sql.add(stringBuilder.toString());
        }

        sqlConnector.execute(sql);

        //Удаляем те, что реже 3 раз
        if (COUNT_WORD_TO_ADD > 1) {
            sql = new ArrayList<>();
            sql.add("delete from " + SQLConnector.TABLE_DICTONARY + " where ID in (select UNIGRAMM_ID from " + SQLConnector.TABLE_SOURCES_IN_UNIGRAM + " group by UNIGRAMM_ID having count(TEXT_ID) < "+COUNT_WORD_TO_ADD+")");
            sql.add("delete from " + SQLConnector.TABLE_SOURCES_IN_UNIGRAM + " where UNIGRAMM_ID not in (select ID from " + SQLConnector.TABLE_DICTONARY + ")");
            sqlConnector.execute(sql);

            //перенумеруем
            resultSet = sqlConnector.getResult("select ID from " + SQLConnector.TABLE_DICTONARY + " order by ID");
            sql = new ArrayList<>();
            i = 1;
            while (resultSet.next()) {
                sql.add("UPDATE " + SQLConnector.TABLE_DICTONARY + " SET ID=" + i + " WHERE ID=" + resultSet.getInt("ID"));
                sql.add("UPDATE " + SQLConnector.TABLE_SOURCES_IN_UNIGRAM + " SET UNIGRAMM_ID=" + i + " WHERE UNIGRAMM_ID=" + resultSet.getInt("ID"));
                i++;
            }
            sqlConnector.execute(sql);
        }
    }

}
