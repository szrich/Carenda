package hu.carenda.app.service;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * Authentikációs logika kezeléséért felelős service réteg.
 * Ez a réteg felel a DAO-k és az üzleti logika (pl. jelszó ellenőrzés) összekapcsolásáért.
 */
public class AuthService {

    private final UserDao userDao = new UserDao();

    /**
     * Megpróbál bejelentkeztetni egy felhasználót a felhasználóneve és jelszava alapján.
     *
     * @param username A felhasználó által megadott felhasználónév.
     * @param password A felhasználó által megadott (sima szöveges) jelszó.
     * @return Egy Optional User objektum, ha a bejelentkezés sikeres; egyébként Optional.empty().
     */
    public Optional<User> login(String username, String password) {
        // Első próbálkozás: biztosítjuk, hogy az adatbázis inicializálva legyen.
        // Ez a hívás hozza létre a táblákat és az alapértelmezett admin felhasználót, ha még nem léteznek.
        Database.get();

        try {
            // A tényleges login logikát egy privát metódusba szervezzük a kódduplikáció elkerülése érdekében.
            return this.performLogin(username, password);

        } catch (RuntimeException ex) {
            // Speciális eset: Ha a hiba "no such table: users", az azt jelenti,
            // hogy az adatbázis-inicializálás valamiért nem futott le sikeresen az első .get() híváskor.
            // Ez egy "önjavító" mechanizmus.
            String msg = ex.getMessage();
            if (msg != null && msg.contains("no such table: users")) {

                // Kényszerítjük az adatbázis újbóli inicializálását (most már biztosan létrehozza a táblát)
                Database.get();

                // És megpróbáljuk a bejelentkezést még egyszer.
                return this.performLogin(username, password);
            }

            // Ha más hiba történt (pl. "database is locked"), azt nem itt kezeljük, dobjuk tovább.
            throw ex;
        }
    }

    /**
     * Elvégzi a tényleges bejelentkeztetési logikát: felhasználó keresése és jelszó ellenőrzése.
     *
     * @param username A felhasználónév.
     * @param password A sima szöveges jelszó.
     * @return Optional<User> sikeres bejelentkezés esetén, egyébként Optional.empty().
     */
    private Optional<User> performLogin(String username, String password) {

        // Elegáns, funkcionális stílusú jelszó-ellenőrzés:
        // 1. Keresd meg a felhasználót a neve alapján.
        // 2. Ha megvan (.filter), ellenőrizd, hogy a jelszó nem null ÉS a BCrypt hash egyezik-e.
        // 3. Ha a filter feltételei teljesülnek, visszaadja az Optional<User>-t.
        // 4. Ha a felhasználó nincs meg, vagy a jelszó null, vagy a hash nem egyezik, a filter
        //    Optional.empty()-t ad vissza.

        return userDao.findByUsername(username)
                .filter(user -> password != null && BCrypt.checkpw(password, user.getPasswordHash()));
    }
}
