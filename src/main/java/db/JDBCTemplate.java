package db;

import api.DelayTask;
import api.History;
import api.HistoryStats;
import api.RecordType;
import manager.Task;
import manager.exeption.ZeroResultException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

public class JDBCTemplate {

    private DataSource dataSource;
    private NamedParameterJdbcTemplate jdbcTemplateObject;
    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplateObject = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
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
                    "SELECT * FROM history" + (nick.equals("admin") ? "" : (" WHERE nick = \'" + nick + "\'")) +
                            (orderBy.isEmpty() ? " ORDER BY `id` DESC " : (" ORDER BY " + orderBy)) +
                            String.format(" LIMIT %d OFFSET %d", pageSize, page * pageSize);

            return jdbcTemplateObject.query(SQL, (rs, rowNum) -> {
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
        } catch (Exception ignored) {
        }

        return new ArrayList<>();
    }

    public HistoryStats getHistoryStats(String nick) {

        try {
            String SQL = "SELECT Count(his.id) AS all_count, one.avg_ten_count, one.avg_ten_time, ten.avg_ten_count, ten.avg_ten_time FROM(SELECT Avg(h.result_count) AS avg_ten_count, Avg(h.TIME) AS avg_ten_time FROM history AS h WHERE h.DATE >= Now() - interval 10 day) AS ten, (SELECT Avg(h.result_count) AS avg_ten_count, Avg(h.TIME) AS avg_ten_time FROM history AS h WHERE h.DATE >= Now() - interval 5 day) AS one, (SELECT * FROM history AS h" + (nick.equals("admin") ? "" : (" WHERE h.nick = \'" + nick + "\'")) + ") AS his";

            List<HistoryStats> all_count = jdbcTemplateObject.query(SQL, (rs, rowNum) -> {
                HistoryStats record = new HistoryStats();
                record.setAllCount(rs.getInt("all_count"));

                record.setAvgOneCount(rs.getInt(2));
                record.setAvgOneTime(rs.getInt(3));

                record.setAvgTenCount(rs.getInt(4));
                record.setAvgTenTime(rs.getInt(5));

                return record;
            });

            return all_count.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new HistoryStats();
    }

    public Integer getDelayTasksCount(String nick) {

        AtomicReference<Integer> result = new AtomicReference<>(0);
        try {
            String SQL = "SELECT Count('id') AS all_count FROM delay_task WHERE nick = '" + nick + "'";

            jdbcTemplateObject.query(SQL, (rs) -> {
                result.set(rs.getInt("all_count"));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.get();
    }

    public List<DelayTask> getDelayTasks(int page, int pageSize, String nick, String orderBy) {

        try {
            String SQL =
                    "SELECT * FROM delay_task" + (" WHERE nick = \'" + nick + "\'") +
                            (orderBy.isEmpty() ? " ORDER BY `id` DESC " : (" ORDER BY " + orderBy)) +
                            String.format(" LIMIT %d OFFSET %d", pageSize, page * pageSize);

            return jdbcTemplateObject.query(SQL, (rs, rowNum) -> {
                DelayTask record = new DelayTask();
                record.setTaksID(rs.getString("id"));
                record.setNick(rs.getString("nick"));
                record.setTitle(rs.getString("title"));
                record.setUrl(rs.getString("report"));
                record.setDate(rs.getTimestamp("date"));

                record.setStatus(DelayTask.getStatus(rs.getString("status")));
                return record;
            });
        } catch (Exception ignored) { }

        return new ArrayList<>();
    }

    public boolean checkDelayTask() {
        String SQL = "SELECT Count('id') FROM delay_task WHERE status = 'QUEUE'";
        return jdbcTemplate.queryForObject(SQL, new Object[]{}, Integer.class) > 0;
    }

    public Task getFreeTask() {

        String SQL = "SELECT * FROM delay_task WHERE status = 'QUEUE' LIMIT 1";

        String taskID = "";
        try {
            DelayTask task = jdbcTemplate.queryForObject(SQL, (rs, rowNum) -> {
                final DelayTask delayTask = new DelayTask();

                delayTask.setTaksID(rs.getString("id"));
                delayTask.setNick(rs.getString("nick"));
                delayTask.setTitle(rs.getString("title"));

                return delayTask;
            });

            taskID = task.getTaksID();
            HashMap<String, ArrayList<String>> params = getTaskParams(taskID);
            return new Task(taskID, task.getNick(), params, Task.Type.DELAY);
        } catch (Exception e) {

            System.out.println("ERORR: " + e.getMessage());

            if (e.getMessage().contains("0 обьявлений"))
                changeDelayTaskStatus(taskID, DelayTask.Status.ZERO_RESULT);
            else
                changeDelayTaskStatus(taskID, DelayTask.Status.FAIL);

            removeDelayTaskParams(taskID);
            return null;
        }
    }

    private HashMap<String, ArrayList<String>> getTaskParams(String taskID) {

        String SQL = String.format("SELECT name, value FROM task_params where task_id = '%s'", taskID);
        HashMap<String, ArrayList<String>> params = new HashMap<>();

        jdbcTemplateObject.query(SQL, rs -> {
            params.put(
                    rs.getString("name"),
                    new ArrayList<>(Collections.singletonList(rs.getString("value"))));
        });

        return params;
    }

    public void changeDelayTaskStatus(String id, DelayTask.Status status) {
        String SQL = "UPDATE delay_task SET status = ? WHERE id = ?";
        jdbcTemplate.update(SQL, status.toString(), id);
    }

    public void createDelayTask(String token, HashMap<String, ArrayList<String>> params) throws ParseException {

        final String INSERT_SQL = "INSERT INTO delay_task (nick, title, date, status) VALUES (?, ?, ?, ?)";

        String title = params.get("title").get(0);
        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(title.substring(title.lastIndexOf("|") + 2));
        final String fTitle = title.replaceAll("\\s\\|\\s\\d+-.*$", "");

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps =
                            connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                    ps.setString(1, token);
                    ps.setString(2, fTitle);
                    ps.setTimestamp(3, new Timestamp(date.getTime()));
                    ps.setString(4, DelayTask.Status.QUEUE.toString());
                    return ps;
                },
                keyHolder);

        insertParams(String.valueOf(keyHolder.getKey()), params);
    }

    private void insertParams(String taskID, HashMap<String, ArrayList<String>> params) {

        String SQL = "INSERT INTO task_params (task_id, name, value) VALUES (?, ?, ?)";
        Iterator<Map.Entry<String, ArrayList<String>>> paramItr = params.entrySet().iterator();

        jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<String, ArrayList<String>> param = paramItr.next();
                ps.setString(1, taskID);
                ps.setString(2, param.getKey());
                ps.setString(3, param.getValue().get(0));

            }

            @Override
            public int getBatchSize() {
                return params.size();
            }
        });
    }

    public void updateDelayTaskReport(String taskID, String resultLink) {
        String SQL = "UPDATE delay_task SET report = ? WHERE id = ?";
        jdbcTemplate.update(SQL, resultLink, taskID);
    }

    public void removeDelayTask(String taskID) {
        String SQL = "DELETE FROM delay_task WHERE id = ?";
        jdbcTemplate.update(SQL, taskID);

        removeDelayTaskParams(taskID);
    }

    public void removeDelayTaskParams(String taskID) {
        final String REMOVE_SQL = "DELETE FROM task_params WHERE task_id = ?";
        jdbcTemplate.update(REMOVE_SQL, taskID);
    }
}
