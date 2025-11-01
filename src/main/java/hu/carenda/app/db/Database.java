package hu.carenda.app.db;

import org.sqlite.SQLiteConfig;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * Singleton adatbázis-kezelő osztály a SQLite kapcsolat biztosítására.
 * Felelős a kapcsolat létrehozásáért, a séma inicializálásáért (egyszeri alkalommal)
 * és az admin felhasználó létrehozásáért (seedelés).
 */
public final class Database {

    /** A globális, perzisztens adatbázis-kapcsolat. */
    private static Connection CONN;

    /**
     * Egy flag, ami jelzi, hogy a sémaellenőrzés (és esetleges inicializálás)
     * már lefutott-e ebben a munkamenetben.
     * Ez megakadályozza, hogy minden egyes .get() híváskor feleslegesen
     * ellenőrizzük a táblák létezését.
     */
    private static boolean schemaInitialized = false;

    /**
     * Privát konstruktor, hogy megakadályozzuk az osztály példányosítását (Utility osztály).
     */
    private Database() {
    }

    /**
     * Visszaadja az egyetlen, globális adatbázis-kapcsolatot.
     * Ha a kapcsolat még nem létezik (vagy lezárult), létrehozza azt,
     * és biztosítja, hogy az adatbázis séma inicializálva legyen.
     *
     * @return Az aktív, szinkronizált Connection objektum.
     * @throws RuntimeException ha az adatbázis-kapcsolatot nem sikerül létrehozni vagy inicializálni.
     */
    public static synchronized Connection get() {
        try {
            if (CONN == null || CONN.isClosed()) {
                // 1. Adatbázis mappa létrehozása (~/.carenda)
                Path dbDir = Path.of(System.getProperty("user.home"), ".carenda");
                try {
                    Files.createDirectories(dbDir);
                } catch (IOException io) {
                    throw new RuntimeException("Nem sikerült létrehozni az adatbázis mappát: " + dbDir, io);
                }

                String url = "jdbc:sqlite:" + dbDir.resolve("carenda.db");

                // 2. SQLite konfiguráció (Idegen kulcsok és timeout)
                SQLiteConfig cfg = new SQLiteConfig();
                cfg.enforceForeignKeys(true);
                cfg.setBusyTimeout(5000); // 5 mp várakozás, ha a DB foglalt

                // 3. Kapcsolat létrehozása
                CONN = DriverManager.getConnection(url, cfg.toProperties());

                // 4. Shutdown hook: Az alkalmazás bezárásakor a kapcsolatot is lezárjuk
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (CONN != null && !CONN.isClosed()) {
                            CONN.close();
                        }
                    } catch (Exception ignored) {
                        // Bezáráskor már nem tudunk mit tenni
                    }
                }));

                System.out.println("[DB] Adatbázis megnyitva: " + url);
            }

            // 5. Séma ellenőrzése (OPTIMALIZÁLVA)
            // Csak akkor fut le, ha még nem ellenőriztük ebben a munkamenetben.
            if (!schemaInitialized) {
                ensureSchema();
                schemaInitialized = true;
            }

            return CONN;

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba (get)", e);
        }
    }

    /**
     * Biztosítja, hogy az adatbázis séma (táblák, nézetek, seed) létezik.
     * Ellenőrzi a 'users' tábla meglétét; ha hiányzik, lefuttatja a teljes
     * séma inicializáló és adatfeltöltő (seed) metódust egy tranzakcióban.
     *
     * @throws SQLException ha a séma ellenőrzése vagy inicializálása sikertelen.
     */
    private static void ensureSchema() throws SQLException {
        // Ellenőrizzük, hogy a 'users' tábla létezik-e. Ha igen, feltételezzük,
        // hogy az egész séma rendben van.
        if (!tableExists(CONN, "users")) {
            System.out.println("[DB] Séma nem található. Inicializálás...");
            
            // Tranzakciót indítunk a teljes séma létrehozásához
            CONN.setAutoCommit(false);
            try {
                initSchemaAndSeed(CONN);
                CONN.commit(); // Ha minden sikerült, véglegesítjük
                System.out.println("[DB] Séma és adatfeltöltés sikeres (OK).");
            } catch (Exception e) {
                try {
                    CONN.rollback(); // Hiba esetén visszavonunk mindent
                    System.err.println("[DB] SÉMA INICIALIZÁLÁS SIKERTELEN! Visszavonás...");
                } catch (Exception ignored) {}
                throw new RuntimeException("Adatbázis séma inicializálási hiba", e);
            } finally {
                try {
                    CONN.setAutoCommit(true); // Mindig visszaállítjuk az auto-commit módot
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Ellenőrzi, hogy egy adott nevű tábla létezik-e az adatbázisban.
     *
     * @param c         Az aktív adatbázis-kapcsolat.
     * @param tableName A keresett tábla neve.
     * @return true, ha a tábla létezik, egyébként false.
     * @throws SQLException ha az adatbázis metaadatainak lekérdezése sikertelen.
     */
    private static boolean tableExists(Connection c, String tableName) throws SQLException {
        final String sql = """
            SELECT name 
              FROM sqlite_master 
             WHERE type='table' AND name=?;
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true, ha van találat (a tábla létezik)
            }
        }
    }

    /**
     * Létrehozza a teljes adatbázis-sémát (táblák, indexek, nézetek)
     * és feltölti az alapértelmezett "admin" felhasználóval.
     *
     * @param c Az aktív adatbázis-kapcsolat (tranzakció alatt).
     * @throws SQLException ha bármelyik SQL parancs végrehajtása sikertelen.
     */
    private static void initSchemaAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA foreign_keys=ON;");

            // --- TÁBLÁK LÉTREHOZÁSA ---

            // USERS
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT UNIQUE NOT NULL,
                  password_hash TEXT NOT NULL,
                  full_name TEXT,
                  role_name TEXT,
                  must_change_password INTEGER NOT NULL DEFAULT 1
                );
                """);

            // CUSTOMERS
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS customers (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL,
                  phone TEXT,
                  email TEXT
                );
                """);

            // VEHICLES
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS vehicles (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  plate TEXT NOT NULL,
                  vin TEXT,
                  engine_no TEXT,
                  brand TEXT,
                  model TEXT,
                  year INTEGER,
                  fuel_type TEXT,
                  customer_id INTEGER,
                  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL
                );
                """);

            // APPOINTMENTS
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS appointments (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  start_ts TEXT NOT NULL,
                  duration INTEGER NOT NULL DEFAULT 60,
                  subject TEXT,
                  note TEXT,
                  status TEXT NOT NULL DEFAULT 'PLANNED',
                  vehicle_id INTEGER,
                  customer_id INTEGER,
                  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,
                  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL
                );
                """);

            // SERVICE JOBCARD (Munkalap)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS servicejobcard (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  jobcard_no TEXT UNIQUE,
                  appointment_id INTEGER,
                  vehicle_id INTEGER,
                  customer_id INTEGER,
                  fault_desc TEXT,
                  repair_note TEXT,
                  diagnosis TEXT,
                  internal_note TEXT,
                  status TEXT NOT NULL DEFAULT 'OPEN',
                  assignee_user_id INTEGER,
                  created_at TEXT NOT NULL,
                  updated_at TEXT,
                  finished_at TEXT,
                  odometer_km INTEGER,
                  fuel_level_eighths INTEGER CHECK(fuel_level_eighths BETWEEN 0 AND 8),
                  currency_code TEXT NOT NULL DEFAULT 'HUF',
                  advance_cents INTEGER NOT NULL DEFAULT 0,
                  FOREIGN KEY(appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
                  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,
                  FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL,
                  FOREIGN KEY(assignee_user_id) REFERENCES users(id) ON DELETE SET NULL
                );
                """);

            // Munkadíjak (Work Description)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS servicejobcard_workdesc (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  sjc_id INTEGER NOT NULL,
                  name TEXT NOT NULL,
                  hours NUMERIC NOT NULL DEFAULT 0,
                  rate_cents INTEGER NOT NULL DEFAULT 0,
                  vat_percent INTEGER NOT NULL DEFAULT 27 CHECK(vat_percent IN (0,5,7,18,27)),
                  sort_order INTEGER NOT NULL DEFAULT 0,
                  FOREIGN KEY(sjc_id) REFERENCES servicejobcard(id) ON DELETE CASCADE
                );
                """);

            // Alkatrészek (Parts)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS servicejobcard_part (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  sjc_id INTEGER NOT NULL,
                  sku TEXT,
                  name TEXT NOT NULL,
                  quantity NUMERIC NOT NULL DEFAULT 1,
                  unit_price_cents INTEGER NOT NULL DEFAULT 0,
                  vat_percent INTEGER NOT NULL DEFAULT 27 CHECK(vat_percent IN (0,5,7,18,27)),
                  sort_order INTEGER NOT NULL DEFAULT 0,
                  FOREIGN KEY(sjc_id) REFERENCES servicejobcard(id) ON DELETE CASCADE
                );
                """);

            // --- INDEXEK LÉTREHOZÁSA ---
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vehicles_plate ON vehicles(plate);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vehicles_customer ON vehicles(customer_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_vehicle ON appointments(vehicle_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_customer ON appointments(customer_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appt_start ON appointments(start_ts);");
            st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS uq_sjc_no ON servicejobcard(jobcard_no);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_appt ON servicejobcard(appointment_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_vehicle ON servicejobcard(vehicle_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sjc_customer ON servicejobcard(customer_id);");

            // --- NÉZET (VIEW) LÉTREHOZÁSA ---
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
        } // Statement 'st' automatikusan bezárul

        // --- ADMIN FELHASZNÁLÓ FELTÖLTÉSE (SEED) ---
        
        // REFAKTORÁLVA: PreparedStatement használata a String összefűzés helyett
        final String seedSql = """
            INSERT INTO users(username, password_hash, full_name, role_name, must_change_password)
            SELECT ?, ?, ?, ?, ?
            WHERE NOT EXISTS(SELECT 1 FROM users WHERE username = 'admin');
            """;
        
        try (PreparedStatement ps = c.prepareStatement(seedSql)) {
            // "admin" jelszó hash-elése
            String adminHash = BCrypt.hashpw("admin", BCrypt.gensalt(10));
            
            ps.setString(1, "admin");
            ps.setString(2, adminHash);
            ps.setString(3, "Rendszergazda");
            ps.setString(4, "ADMIN");
            ps.setInt(5, 1); // must_change_password = true
            
            ps.executeUpdate();
        }
    }
}
