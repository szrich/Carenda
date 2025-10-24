package hu.carenda.app.service;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;
import org.mindrot.jbcrypt.BCrypt; // <-- ÚJ

import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public Optional<User> login(String username, String password) {
        // Biztosítjuk, hogy az adatbázis felálljon (létrehozza a táblákat, beleteszi az admin usert stb.)
        Database.get();

        try {
            Optional<User> rec = userDao.findByUsername(username);

            if (rec.isEmpty() || password == null) {
                return Optional.empty();
            }

            User u = rec.get();

            // NEM sima equals összehasonlítás,
            // hanem a beírt jelszót ellenőrizzük a tárolt bcrypt hash ellen
            if (BCrypt.checkpw(password, u.getPasswordHash())) {
                // Sikeres belépés
                return Optional.of(u);
            } else {
                // Hibás jelszó
                return Optional.empty();
            }

        } catch (RuntimeException ex) {
            String msg = String.valueOf(ex.getMessage());

            // Ha valami miatt a 'users' tábla nem állt fel elsőre (elég extrém eset),
            // akkor még egyszer megpróbáljuk.
            if (msg.contains("no such table: users")) {
                Database.get();
                Optional<User> rec = userDao.findByUsername(username);

                if (rec.isEmpty() || password == null) {
                    return Optional.empty();
                }

                User u = rec.get();
                if (BCrypt.checkpw(password, u.getPasswordHash())) {
                    return Optional.of(u);
                } else {
                    return Optional.empty();
                }
            }

            // egyéb hibát továbbdobjuk
            throw ex;
        }
    }
}
