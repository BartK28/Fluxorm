package nl.seyox.mysql;

import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import nl.seyox.FluxormDriver;
import nl.seyox.FluxormSettings;
import nl.seyox.annotations.JsonColumn;
import nl.seyox.models.Model;
import nl.seyox.structure.FluxormResult;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HikariFluxormDriverImpl implements FluxormDriver {

    private HikariConfig config = new HikariConfig();
    private HikariDataSource ds;
    private FluxormSettings settings;

    public HikariFluxormDriverImpl(FluxormSettings settings) {
        this.settings = settings;
        config.setJdbcUrl(settings.getUrl());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(100);
        ds = new HikariDataSource(config);
        createDatabase();
        ds.close();

        config.setJdbcUrl(settings.getUrl() + "/" + settings.getDatabase());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(100);
        ds = new HikariDataSource(config);
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void createDatabase() {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("CREATE DATABASE IF NOT EXISTS " + settings.getDatabase())) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T extends Model> FluxormResult<T> where(Class<T> model, String field, String operator, Object value) {
        try {
            T t = model.getConstructor().newInstance();
            String sql = "SELECT * FROM `" + t.getTableName() + "` WHERE `" + field + "` " + operator + " ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, value);
                try (ResultSet result = stmt.executeQuery()) {
                    FluxormResult<T> fluxormResult = new FluxormResult<>(List.of());
                    while (result.next()) {
                        t = model.getConstructor().newInstance();
                        for (Field declaredField : t.getClass().getDeclaredFields()) {
                            declaredField.setAccessible(true);
                            if (declaredField.getType() == String.class) {
                                declaredField.set(t, result.getString(declaredField.getName()));
                            } else if (declaredField.getType() == Integer.class || declaredField.getType() == int.class) {
                                declaredField.set(t, result.getInt(declaredField.getName()));
                            } else if (declaredField.getType() == Long.class || declaredField.getType() == long.class) {
                                declaredField.set(t, result.getLong(declaredField.getName()));
                            } else if (declaredField.getType() == Double.class || declaredField.getType() == double.class) {
                                declaredField.set(t, result.getDouble(declaredField.getName()));
                            } else if (declaredField.getType() == Boolean.class || declaredField.getType() == boolean.class) {
                                declaredField.set(t, result.getBoolean(declaredField.getName()));
                            } else if (declaredField.getType() == Float.class || declaredField.getType() == float.class) {
                                declaredField.set(t, result.getFloat(declaredField.getName()));
                            } else if (declaredField.isAnnotationPresent(JsonColumn.class)) {
                                declaredField.set(t, new Gson().fromJson(result.getString(declaredField.getName()), declaredField.getType()));
                            }
                            declaredField.setAccessible(false);
                        }
                        Field id = t.getClass().getSuperclass().getDeclaredField("id");
                        id.setAccessible(true);
                        id.set(t, result.getInt("id"));
                        id.setAccessible(false);
                        fluxormResult.add(t);
                    }
                    return fluxormResult;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void createTable(Model model) {
        String tableName = model.getTableName();

        try (Connection conn = ds.getConnection()) {
            // 1. Kijk of de tabel al bestaat
            boolean tableExists;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?")) {
                stmt.setString(1, settings.getDatabase());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    tableExists = rs.getInt(1) > 0;
                }
            }

            if (!tableExists) {
                // 2. Tabel bestaat niet, dus gewoon aanmaken
                String createSql = "CREATE TABLE `" + tableName + "` (" +
                        model.getColumns().stream()
                                .map(c -> "`" + c.getName() + "` " + c.getType())
                                .collect(Collectors.joining(", ")) +
                        ");";

                try (PreparedStatement stmt = conn.prepareStatement(createSql)) {
                    stmt.execute();
                }

            } else {
                // 3. Tabel bestaat, dus synchroniseren
                // Haal bestaande kolommen op
                PreparedStatement colStmt = conn.prepareStatement(
                        "SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?");
                colStmt.setString(1, settings.getDatabase());
                colStmt.setString(2, tableName);

                ResultSet rs = colStmt.executeQuery();
                Map<String, String> existingColumns = new HashMap<>();
                while (rs.next()) {
                    existingColumns.put(rs.getString("COLUMN_NAME").toLowerCase(), rs.getString("COLUMN_TYPE").toLowerCase());
                }
                rs.close();
                colStmt.close();

                // Vergelijk met model-kolommen
                for (var col : model.getColumns()) {
                    String name = col.getName().toLowerCase();
                    String type = col.getType().toLowerCase();

                    if (!existingColumns.containsKey(name)) {
                        // Kolom bestaat niet → toevoegen
                        String alter = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + col.getName() + "` " + col.getType();
                        try (PreparedStatement stmt = conn.prepareStatement(alter)) {
                            stmt.execute();
                        }
                    } else if (!existingColumns.get(name).equals(type) && !name.equals("id")) {
                        // Kolom bestaat maar type is anders → aanpassen
                        String alter = "ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + col.getName() + "` " + col.getType();
                        try (PreparedStatement stmt = conn.prepareStatement(alter)) {
                            stmt.execute();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public <T extends Model> FluxormResult<T> all(Class<T> model) {
        try {
            T t = model.getConstructor().newInstance();
            String sql = "SELECT * FROM `" + t.getTableName() + "`;";
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet result = stmt.executeQuery()) {

                FluxormResult<T> fluxormResult = new FluxormResult<>(List.of());
                while (result.next()) {
                    t = model.getConstructor().newInstance();
                    for (Field declaredField : t.getClass().getDeclaredFields()) {
                        declaredField.setAccessible(true);
                        if (declaredField.getType() == String.class) {
                            declaredField.set(t, result.getString(declaredField.getName()));
                        } else if (declaredField.getType() == Integer.class || declaredField.getType() == int.class) {
                            declaredField.set(t, result.getInt(declaredField.getName()));
                        } else if (declaredField.getType() == Long.class || declaredField.getType() == long.class) {
                            declaredField.set(t, result.getLong(declaredField.getName()));
                        } else if (declaredField.getType() == Double.class || declaredField.getType() == double.class) {
                            declaredField.set(t, result.getDouble(declaredField.getName()));
                        } else if (declaredField.getType() == Boolean.class || declaredField.getType() == boolean.class) {
                            declaredField.set(t, result.getBoolean(declaredField.getName()));
                        } else if (declaredField.getType() == Float.class || declaredField.getType() == float.class) {
                            declaredField.set(t, result.getFloat(declaredField.getName()));
                        } else if (declaredField.isAnnotationPresent(JsonColumn.class)) {
                            declaredField.set(t, new Gson().fromJson(result.getString(declaredField.getName()), declaredField.getType()));
                        }
                        declaredField.setAccessible(false);
                    }
                    Field id = t.getClass().getSuperclass().getDeclaredField("id");
                    id.setAccessible(true);
                    id.set(t, result.getInt("id"));
                    id.setAccessible(false);
                    fluxormResult.add(t);
                }
                return fluxormResult.isEmpty() ? null : fluxormResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void save(Model model) {
        Field[] fields = model.getClass().getDeclaredFields();
        List<Field> nonJsonFields = Arrays.stream(fields)
                .filter(f -> !f.isAnnotationPresent(JsonColumn.class))
                .collect(Collectors.toList());

        if (model.getId() == -1) {
            // INSERT
            String columnNames = nonJsonFields.stream()
                    .map(Field::getName)
                    .collect(Collectors.joining(", "));

            String placeholders = nonJsonFields.stream()
                    .map(f -> "?")
                    .collect(Collectors.joining(", "));

            String sql = "INSERT INTO `" + model.getTableName() + "` (" + columnNames + ") VALUES (" + placeholders + ")";

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                for (int i = 0; i < nonJsonFields.size(); i++) {
                    Field f = nonJsonFields.get(i);
                    f.setAccessible(true);
                    stmt.setObject(i + 1, f.get(model));
                    f.setAccessible(false);
                }

                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        model.setId(keys.getInt(1));
                    }
                }

            } catch (SQLException | IllegalAccessException e) {
                e.printStackTrace();
            }

        } else {
            // UPDATE
            String setClause = nonJsonFields.stream()
                    .map(f -> f.getName() + " = ?")
                    .collect(Collectors.joining(", "));

            String sql = "UPDATE `" + model.getTableName() + "` SET " + setClause + " WHERE id = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (int i = 0; i < nonJsonFields.size(); i++) {
                    Field f = nonJsonFields.get(i);
                    f.setAccessible(true);
                    stmt.setObject(i + 1, f.get(model));
                    f.setAccessible(false);
                }

                stmt.setInt(nonJsonFields.size() + 1, model.getId());
                stmt.executeUpdate();

            } catch (SQLException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


}
