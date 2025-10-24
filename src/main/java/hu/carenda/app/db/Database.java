package hu.carenda.app.db;

import java.io.IOException;
import org.sqlite.SQLiteConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import org.mindrot.jbcrypt.BCrypt;

public final class Database {

    private static Connection CONN;

    private Database() {
    }

    public static synchronized Connection get() {
        try {
            if (CONN == null || CONN.isClosed()) {
                // ~/.carenda mappa létrehozása
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

                // ha kilépünk az appból, zárjuk le szépen
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

            // NAGY ÚJDONSÁG:
            // minden egyes get() híváskor ellenőrizzük, hogy a séma megvan-e.
            // ha nincs, most azonnal felépítjük és seedeljük.
            ensureSchema();

            return CONN;

        } catch (SQLException e) {
            throw new RuntimeException("DB open/init error", e);
        }
    }

    /**
     * Ez megnézi, hogy létezik-e legalább a 'users' tábla.
     * Ha nem, akkor felépíti az egész sémát és beteszi az admin usert.
     */
    private static void ensureSchema() {
        try {
            if (!tableExists(CONN, "users")) {
                System.out.println("[DB] Schema not found. Initializing...");
                CONN.setAutoCommit(false);
                try {
                    initSchemaAndSeed(CONN);
                    CONN.commit();
                    System.out.println("[DB] Schema OK, seed OK");
                } catch (Exception e) {
                    try {
                        CONN.rollback();
                    } catch (Exception ignored) {}
                    throw new RuntimeException("DB schema init failed", e);
                } finally {
                    try {
                        CONN.setAutoCommit(true);
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("DB schema check failed", ex);
        }
    }

    /**
     * Megnézzük, hogy adott tábla létezik-e az adatbázisban.
     */
    private static boolean tableExists(Connection c, String tableName) throws SQLException {
        final String q = "SELECT name FROM sqlite_master WHERE type='table' AND name=?;";
        try (PreparedStatement ps = c.prepareStatement(q)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Létrehozza a teljes sémát (táblák, nézetek, indexek) és beszúrja az admin felhasználót.
     * Itt tételezzük fel, hogy a tranzakció már el van kezdve (autoCommit=false a hívónál),
     * és a hívó commitál vagy rollbackel.
     */
    private static void initSchemaAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA foreign_keys=ON;");

            // USERS tábla (bcrypt + kötelező jelszócsere flag)
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  username TEXT UNIQUE NOT NULL,"
                + "  password_hash TEXT NOT NULL,"
                + "  full_name TEXT,"
                + "  role_name TEXT,"
                + "  must_change_password INTEGER NOT NULL DEFAULT 1"
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

            // VEHICLES
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS vehicles ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  plate TEXT NOT NULL,"           // rendszám
                + "  vin TEXT,"                       // alvázszám
                + "  engine_no TEXT,"                 // motorszám
                + "  brand TEXT,"                     // gyártmány
                + "  model TEXT,"                     // típus
                + "  year INTEGER,"                   // évjárat
                + "  fuel_type TEXT,"                 // üzemanyag
                + "  customer_id INTEGER,"
                + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL"
                + ");"
            );

            // APPOINTMENTS
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS appointments ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  start_ts TEXT NOT NULL,"                // ISO dátum/idő
                + "  duration INTEGER NOT NULL DEFAULT 60,"  // perc
                + "  subject TEXT,"
                + "  note TEXT,"
                + "  status TEXT NOT NULL DEFAULT 'PLANNED',"
                + "  vehicle_id INTEGER,"
                + "  customer_id INTEGER,"
                + "  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,"
                + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL"
                + ");"
            );

            // SERVICE JOBCARD (munkalap)
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS servicejobcard ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  jobcard_no TEXT UNIQUE,"              // pl. 2025-000123
                + "  appointment_id INTEGER,"
                + "  vehicle_id INTEGER,"
                + "  customer_id INTEGER,"
                + "  fault_desc TEXT,"                     // hiba leírás
                + "  repair_note TEXT,"                    // javítási megjegyzés
                + "  diagnosis TEXT,"                      // diagnózis
                + "  internal_note TEXT,"                  // belső megjegyzés
                + "  status TEXT NOT NULL DEFAULT 'OPEN'," // OPEN / IN_PROGRESS / READY / DELIVERED / CANCELLED
                + "  assignee_user_id INTEGER,"            // felelős szerelő (users.id)
                + "  created_at TEXT NOT NULL,"            // létrehozás ideje
                + "  updated_at TEXT,"                     // módosítás ideje
                + "  finished_at TEXT,"                    // elkészülés ideje
                + "  odometer_km INTEGER,"
                + "  fuel_level_eighths INTEGER CHECK(fuel_level_eighths BETWEEN 0 AND 8),"
                + "  currency_code TEXT NOT NULL DEFAULT 'HUF',"
                + "  advance_cents INTEGER NOT NULL DEFAULT 0,"
                + "  FOREIGN KEY(appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,"
                + "  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,"
                + "  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL,"
                + "  FOREIGN KEY(assignee_user_id) REFERENCES users(id) ON DELETE SET NULL"
                + ");"
            );

            // Munkadíjak a munkalapon
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS servicejobcard_workdesc ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  sjc_id INTEGER NOT NULL,"      // hivatkozás servicejobcard.id-re
                + "  name TEXT NOT NULL,"           // pl. 'Fékbetét csere'
                + "  hours NUMERIC NOT NULL DEFAULT 0,"      // 1.5 óra stb.
                + "  rate_cents INTEGER NOT NULL DEFAULT 0," // Ft/óra fillérben
                + "  vat_percent INTEGER NOT NULL DEFAULT 27 CHECK(vat_percent IN (0,5,7,18,27)),"
                + "  sort_order INTEGER NOT NULL DEFAULT 0,"
                + "  FOREIGN KEY(sjc_id) REFERENCES servicejobcard(id) ON DELETE CASCADE"
                + ");"
            );

            // Alkatrészek a munkalapon
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS servicejobcard_part ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  sjc_id INTEGER NOT NULL,"
                + "  sku TEXT,"                               // cikkszám
                + "  name TEXT NOT NULL,"                     // megnevezés
                + "  quantity NUMERIC NOT NULL DEFAULT 1,"    // darabszám / mennyiség
                + "  unit_price_cents INTEGER NOT NULL DEFAULT 0,"
                + "  vat_percent INTEGER NOT NULL DEFAULT 27 CHECK(vat_percent IN (0,5,7,18,27)),"
                + "  sort_order INTEGER NOT NULL DEFAULT 0,"
                + "  FOREIGN KEY(sjc_id) REFERENCES servicejobcard(id) ON DELETE CASCADE"
                + ");"
            );

            // INDEXEK
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vehicles_plate ON vehicles(plate);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vehicles_customer ON vehicles(customer_id);");

            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_vehicle ON appointments(vehicle_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_customer ON appointments(customer_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_start ON appointments(start_ts);");

            st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS uq_sjc_no ON servicejobcard(jobcard_no);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_appt ON servicejobcard(appointment_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_vehicle ON servicejobcard(vehicle_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_customer ON servicejobcard(customer_id);");

            // VIEW (összesített pénzügy)
            st.executeUpdate("DROP VIEW IF EXISTS view_sjc_totals;");
            st.executeUpdate("""
                CREATE VIEW view_sjc_totals AS
                WITH work_desc AS (
                  SELECT
                    sjc_id,
                    CAST(ROUND(hours * rate_cents) AS INTEGER) AS net_cents,
                    CAST(ROUND(hours * rate_cents * vat_percent / 100.0) AS INTEGER) AS vat_cents
                  FROM servicejobcard_workdesc
                ),
                work_desc_sum AS (
                  SELECT sjc_id,
                         SUM(net_cents) AS work_net_cents,
                         SUM(vat_cents) AS work_vat_cents
                  FROM work_desc
                  GROUP BY sjc_id
                ),
                parts AS (
                  SELECT
                    sjc_id,
                    CAST(ROUND(quantity * unit_price_cents) AS INTEGER) AS net_cents,
                    CAST(ROUND(quantity * unit_price_cents * vat_percent / 100.0) AS INTEGER) AS vat_cents
                  FROM servicejobcard_part
                ),
                parts_sum AS (
                  SELECT sjc_id,
                         SUM(net_cents) AS parts_net_cents,
                         SUM(vat_cents) AS parts_vat_cents
                  FROM parts
                  GROUP BY sjc_id
                )
                SELECT
                  s.id AS sjc_id,
                  IFNULL(w.work_net_cents,0) + IFNULL(p.parts_net_cents,0) AS subtotal_net_cents,
                  IFNULL(w.work_vat_cents,0) + IFNULL(p.parts_vat_cents,0) AS vat_cents,
                  (IFNULL(w.work_net_cents,0) + IFNULL(p.parts_net_cents,0)
                   + IFNULL(w.work_vat_cents,0) + IFNULL(p.parts_vat_cents,0)) AS total_gross_cents,
                  s.advance_cents,
                  (IFNULL(w.work_net_cents,0) + IFNULL(p.parts_net_cents,0)
                   + IFNULL(w.work_vat_cents,0) + IFNULL(p.parts_vat_cents,0))
                   - IFNULL(s.advance_cents,0) AS amount_due_cents
                FROM servicejobcard s
                LEFT JOIN work_desc_sum w ON w.sjc_id = s.id
                LEFT JOIN parts_sum p ON p.sjc_id = s.id;
            """);

            // --- ADMIN FELHASZNÁLÓ FELTÖLTÉSE BCRYPT HASH-EL ---

            // legeneráljuk az "admin" jelszó hash-ét Bcrypt-tel
            String adminHash = BCrypt.hashpw("admin", BCrypt.gensalt(10));

            // beszúrjuk az admin usert, ha még nincs
            // must_change_password = 1 --> első belépés után kötelező új jelszót megadnia
            String seedSql =
                "INSERT INTO users(username, password_hash, full_name, role_name, must_change_password) " +
                "SELECT 'admin','" + adminHash + "','Rendszergazda','ADMIN',1 " +
                "WHERE NOT EXISTS(SELECT 1 FROM users WHERE username='admin');";

            st.executeUpdate(seedSql);
        }
    }
}
