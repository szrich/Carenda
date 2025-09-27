package hu.carenda.app.service;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;

import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public Optional<User> login(String username, String password) {
        // Biztosítsuk az initet (megnyitja a kapcsolatot és felépíti a sémát)
        Database.get();

        try {
            Optional<User> rec = userDao.findByUsername(username);
            if (rec.isPresent() && password != null && password.equals(rec.get().getPasswordHash())) {
                return rec;
            }
            return Optional.empty();

        } catch (RuntimeException ex) {
            String msg = String.valueOf(ex.getMessage());
            // Ha a DB mégsem állt fel és hiányzik a users tábla, próbáljuk meg még egyszer
            if (msg.contains("no such table: users")) {
                Database.get();
                Optional<User> rec = userDao.findByUsername(username);
                if (rec.isPresent() && password != null && password.equals(rec.get().getPasswordHash())) {
                    return rec;
                }
                return Optional.empty();
            }
            throw ex;
        }
    }
}
