package it.cnr.isti.labsedc.concern.storage;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserDAO {

    public static class UserRecord {
        public String username;
        public String passwordHash;
        public String role;
    }

    private static Connection authConn;
    private static boolean setupDone = false;

    /**
     * Returns a live connection, creating it on first call.
     * On the very first successful connection also ensures the users table
     * exists and seeds the default accounts — this makes the class self-healing
     * even when the MySQL volume pre-dates the addition of the users table.
     */
    private static synchronized Connection getConnection() throws Exception {
        if (authConn == null || authConn.isClosed()) {
            String host = System.getenv().getOrDefault("MYSQL_HOST", "localhost");
            int port = Integer.parseInt(System.getenv().getOrDefault("MYSQL_PORT", "3306"));
            String user = System.getenv().getOrDefault("MYSQL_USER", "concern");
            String pass = System.getenv().getOrDefault("MYSQL_PASSWORD", "un53cur3!!");
            String db   = System.getenv().getOrDefault("MYSQL_DATABASE", "eventdb");
            Class.forName("com.mysql.cj.jdbc.Driver");
            authConn = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + db, user, pass);
            setupDone = false; // re-run setup after reconnect
        }
        if (!setupDone) {
            ensureSetup(authConn);
            setupDone = true;
        }
        return authConn;
    }

    /**
     * Idempotent: creates the users table if absent and seeds default accounts
     * when the table is empty.  Safe to call on every startup.
     */
    private static void ensureSetup(Connection conn) {
        try {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS `users` (" +
                "  `id`            BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT," +
                "  `username`      VARCHAR(100)         NOT NULL," +
                "  `password_hash` VARCHAR(255)         NOT NULL," +
                "  `role`          ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER'," +
                "  `created_at`    TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  `updated_at`    TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                "                                                ON UPDATE CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (`id`)," +
                "  UNIQUE KEY `uq_username` (`username`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
            );

            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            boolean empty = rs.getInt(1) == 0;
            rs.close();

            if (empty) {
                insertUser(conn, "admin", "admin123", "ADMIN");
                insertUser(conn, "user",  "user123",  "USER");
                System.out.println("[UserDAO] Default users seeded: admin/admin123 (ADMIN), user/user123 (USER)");
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] ensureSetup error: " + e.getMessage());
        }
    }

    public static UserRecord findUser(String username) {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT username, password_hash, role FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                UserRecord u = new UserRecord();
                u.username     = rs.getString("username");
                u.passwordHash = rs.getString("password_hash");
                u.role         = rs.getString("role");
                rs.close();
                stmt.close();
                return u;
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            System.err.println("[UserDAO] findUser error: " + e.getMessage());
        }
        return null;
    }

    public static boolean checkPassword(String plain, String hashed) {
        try {
            return BCrypt.checkpw(plain, hashed);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Called from MySQLStorageController.connectToDB() so the main monitoring
     * connection also triggers setup (useful when monitoring starts before any
     * login attempt).
     */
    public static void initDefaultUsers(Connection conn) {
        ensureSetup(conn);
    }

    private static void insertUser(Connection conn, String username, String password, String role)
            throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
        stmt.setString(3, role);
        stmt.execute();
        stmt.close();
    }
}
