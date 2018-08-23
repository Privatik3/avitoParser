package db;

import api.History;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

public class JDBCTemplate {

    private DataSource dataSource;
    private NamedParameterJdbcTemplate jdbcTemplateObject;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplateObject = new NamedParameterJdbcTemplate(dataSource);
    }

    public void createHistoryRecord(History record) {

        String SQL =
                "INSERT into history (ip, nick, title, report, result_count, time) " +
                "VALUES (:ip, :nick, :title, :report, :result_count, :time);";

        MapSqlParameterSource parameter = new MapSqlParameterSource();
        parameter.addValue("ip", record.getIp());
        parameter.addValue("nick", record.getNick());
        parameter.addValue("title", record.getTitle());
        parameter.addValue("report", record.getUrl());
        parameter.addValue("result_count", record.getResultCount());
        parameter.addValue("time", record.getTime());

        jdbcTemplateObject.update(SQL, parameter);
    }

    public List<History> getHistory(String nick) {

        String SQL =
                "SELECT * FROM history" + (nick.equals("admin") ? "" : " WHERE nick = :nick") +
                " ORDER BY `id` DESC limit 0, 100";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("nick", nick);

        return jdbcTemplateObject.query(SQL, parameters, (rs, rowNum) -> {
            History record = new History();
            record.setIp(rs.getString("ip"));
            record.setNick(rs.getString("nick"));
            record.setTitle(rs.getString("title"));
            record.setUrl(rs.getString("report"));
            record.setResultCount(rs.getInt("result_count"));
            record.setTime(rs.getInt("time"));

            return record;
        });
    }
}
