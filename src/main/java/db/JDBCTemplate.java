package db;

import api.History;
import api.RecordType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
                "INSERT into history (ip, nick, result_count, time, title, report, date, type) " +
                        "VALUES (:ip, :nick, :result_count, :time, :title, :report, :date, :type);";

        MapSqlParameterSource parameter = new MapSqlParameterSource();
        parameter.addValue("ip", record.getIp());
        parameter.addValue("nick", record.getNick());
        parameter.addValue("result_count", record.getResultCount());
        parameter.addValue("time", record.getTime());
        parameter.addValue("title", record.getTitle());
        parameter.addValue("report", record.getUrl());
        parameter.addValue("date", record.getDate());
        parameter.addValue("type", record.getType().toString());

        jdbcTemplateObject.update(SQL, parameter);
    }

    public List<History> getHistory(int page, int pageSize, String nick, String orderBy) {

        try {
            String SQL =
                    "SELECT * FROM history" + (nick.equals("admin") ? "" : " WHERE nick = :nick") +
                            (orderBy.isEmpty() ? " ORDER BY `id` DESC " : (" ORDER BY " + orderBy)) +
                            String.format(" LIMIT %d OFFSET %d", pageSize, page * pageSize );

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("nick", nick);

            return jdbcTemplateObject.query(SQL, parameters, (rs, rowNum) -> {
                History record = new History();
                record.setIp(rs.getString("ip"));
                record.setNick(rs.getString("nick"));
                record.setResultCount(rs.getInt("result_count"));
                record.setTime(rs.getInt("time"));
                record.setTitle(rs.getString("title"));
                record.setUrl(rs.getString("report"));
                record.setDate(rs.getTimestamp("date"));
                record.setType(RecordType.getType(rs.getString("type")));

                return record;
            });
        } catch (Exception ignored) {}

        return new ArrayList<>();
    }
}
