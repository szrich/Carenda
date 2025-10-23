package hu.carenda.app.db;

import java.io.IOException;
import org.sqlite.SQLiteConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public final class Database {

    private static Connection CONN;

    private Database() {
    }

    public static synchronized Connection get() {
        try {
            if (CONN == null || CONN.isClosed()) {
                Path dbDir = Path.of(System.getProperty("user.home"), ".carenda");
                try {
                    Files.createDirectories(dbDir);
                } catch (IOException io) {
                    throw new RuntimeException("Nem sikerült létrehozni a DB mappát: " + dbDir, io);
                }

                String url = "jdbc:sqlite:" + dbDir.resolve("carenda.db");

                SQLiteConfig cfg = new SQLiteConfig();
                cfg.enforceForeignKeys(true);
                cfg.setBusyTimeout(5000);

                CONN = DriverManager.getConnection(url, cfg.toProperties());

                // Tranzakcióban építjük fel a sémát
                CONN.setAutoCommit(false);
                try {
                    initSchemaAndSeed(CONN);
                    CONN.commit();
                } catch (Exception e) {
                    try {
                        CONN.rollback();
                    } catch (Exception ignored) {
                    }
                    throw e;
                } finally {
                    try {
                        CONN.setAutoCommit(true);
                    } catch (Exception ignored) {
                    }
                }

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (CONN != null && !CONN.isClosed()) {
                            CONN.close();
                        }
                    } catch (Exception ignored) {
                    }
                }));

                System.out.println("[DB] Opened: " + url);
            }
            return CONN;
        } catch (SQLException e) {
            throw new RuntimeException("DB open/init error", e);
        }
    }

    /*
    private static void initSchemaAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {

            st.executeUpdate("PRAGMA foreign_keys=ON;");

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  username TEXT UNIQUE NOT NULL,"
                    + "  password_hash TEXT NOT NULL,"
                    + "  full_name TEXT,"
                    + "  role_name TEXT"
                    + ");"
            );

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS customers ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  name TEXT NOT NULL,"
                    + "  phone TEXT,"
                    + "  email TEXT"
                    + ");"
            );

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS vehicles ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  plate TEXT NOT NULL,"
                    + "  make_model TEXT,"
                    + "  customer_id INTEGER,"
                    + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL"
                    + ");"
            );

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS appointments ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  start_ts TEXT NOT NULL,"
                    + "  duration INTEGER NOT NULL DEFAULT 60,"
                    + "  subject TEXT,"
                    + "  note TEXT,"
                    + "  status TEXT NOT NULL DEFAULT 'PLANNED',"
                    + "  vehicle_id INTEGER,"
                    + "  customer_id INTEGER,"
                    + "  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,"
                    + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL"
                    + ");"
            );
            // Seed admin/admin – csak ha nincs még
            st.executeUpdate(
                    "INSERT INTO users(username, password_hash, full_name, role_name) "
                    + "SELECT 'admin','admin','Rendszergazda','ADMIN' "
                    + "WHERE NOT EXISTS(SELECT 1 FROM users WHERE username='admin');"
            );

            System.out.println("[DB] Schema OK, seed OK");
        }
    }*/
    private static void initSchemaAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA foreign_keys=ON;");

            // USERS
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  username TEXT UNIQUE NOT NULL,"
                    + "  password_hash TEXT NOT NULL,"
                    + "  full_name TEXT,"
                    + "  role_name TEXT"
                    + ");"
            );

            // CUSTOMERS
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS customers ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  name TEXT NOT NULL,"
                    + "  phone TEXT,"
                    + "  email TEXT"
                    + ");"
            );

            // VEHICLES – bővített mezőkkel
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS vehicles ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  plate TEXT NOT NULL,"
                    + // rendszám
                    "  vin TEXT,"
                    + // alvázszám
                    "  engine_no TEXT,"
                    + // motorszám
                    "  brand TEXT,"
                    + // gyártmány
                    "  model TEXT,"
                    + // típus
                    "  year INTEGER,"
                    + // évjárat
                    "  odometer_km INTEGER,"
                    + // futás
                    "  fuel_type TEXT,"
                    + // üzemanyag
                    "  customer_id INTEGER,"
                    + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL"
                    + ");"
            );

            // APPOINTMENTS – status maradhat PLANNED/CONFIRMED/DONE/CANCELLED,
            // ha magyarul használod (TERVEZETT/LEMONDOTT/BEFEJEZETT), az app kezeli a szöveget
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS appointments ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  start_ts TEXT NOT NULL,"
                    + // ISO LocalDateTime string
                    "  duration INTEGER NOT NULL DEFAULT 60,"
                    + "  subject TEXT,"
                    + "  note TEXT,"
                    + "  status TEXT NOT NULL DEFAULT 'PLANNED',"
                    + "  vehicle_id INTEGER,"
                    + "  customer_id INTEGER,"
                    + "  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,"
                    + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL"
                    + ");"
            );

            // ÚJ: SERVICEJOBCARD (munkalap)
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS servicejobcard ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  jobcard_no TEXT UNIQUE,"
                    + // emberbarát azonosító (pl. 2025-000123)
                    "  appointment_id INTEGER,"
                    + "  vehicle_id INTEGER,"
                    + "  customer_id INTEGER,"
                    + "  fault_desc TEXT,"
                    + // hiba leírás
                    "  repair_note TEXT,"
                    + // javítási megjegyzés
                    "  work_done TEXT,"
                    + // elvégzett munka
                    "  parts_used TEXT,"
                    + // felhasznált alkatrészek (később lehet külön tábla)
                    "  created_at TEXT,"
                    + "  updated_at TEXT,"
                    + "  FOREIGN KEY(appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,"
                    + "  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,"
                    + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL"
                    + ");"
            );

            // INDEXEK (ajánlott)
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vehicles_plate ON vehicles(plate);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vehicles_customer ON vehicles(customer_id);");

            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_vehicle ON appointments(vehicle_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_customer ON appointments(customer_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_start ON appointments(start_ts);");

            st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS uq_sjc_no ON servicejobcard(jobcard_no);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_appt ON servicejobcard(appointment_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_vehicle ON servicejobcard(vehicle_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_customer ON servicejobcard(customer_id);");

            // Seed admin
            st.executeUpdate(
                    "INSERT INTO users(username, password_hash, full_name, role_name) "
                    + "SELECT 'admin','admin','Rendszergazda','ADMIN' "
                    + "WHERE NOT EXISTS(SELECT 1 FROM users WHERE username='admin');"
            );

            System.out.println("[DB] Schema OK, seed OK");
        }
    }

}
