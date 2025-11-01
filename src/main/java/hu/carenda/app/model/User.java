package hu.carenda.app.model;

/**
 * Egyszerű User modell (POJO) az authentikációhoz és a felhasználói adatok tárolásához.
 */
public class User {

    private int id;
    private String username;
    private String passwordHash; // A jelszó hash-elt változata
    private String fullName;

    // Szerep információk (opcionálisak, lehetnek null-ok)
    private Integer roleId;
    private String roleName;

    // Flag, hogy a felhasználónak kötelező-e jelszót cserélnie
    private boolean mustChangePassword;

    /**
     * Alapértelmezett konstruktor.
     */
    public User() {
    }

    /**
     * Teljes konstruktor az összes mező beállításához.
     */
    public User(int id, String username, String passwordHash, String fullName, Integer roleId, String roleName, boolean mustChangePassword) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roleId = roleId;
        this.roleName = roleName;
        this.mustChangePassword = mustChangePassword;
    }

    // --- Getters and Setters ---

    /**
     * @return A felhasználó egyedi azonosítója (ID).
     */
    public int getId() {
        return id;
    }

    /**
     * Beállítja a felhasználó ID-ját.
     * @param id A felhasználó ID-ja.
     * @return Maga a User objektum (fluent interface).
     */
    public User setId(int id) {
        this.id = id;
        return this;
    }

    /**
     * @return A felhasználónév (login név).
     */
    public String getUsername() {
        return username;
    }

    /**
     * Beállítja a felhasználónevet.
     * @param username A felhasználónév.
     * @return Maga a User objektum (fluent interface).
     */
    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * @return A jelszó BCrypt hash-e.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Beállítja a jelszó hash-t.
     * @param passwordHash Az eltárolt jelszó hash.
     * @return Maga a User objektum (fluent interface).
     */
    public User setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    /**
     * @return A felhasználó teljes neve.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Beállítja a felhasználó teljes nevét.
     * @param fullName A teljes név.
     * @return Maga a User objektum (fluent interface).
     */
    public User setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    /**
     * @return A felhasználó szerepkörének ID-ja.
     */
    public Integer getRoleId() {
        return roleId;
    }

    /**
     * Beállítja a felhasználó szerepkörének ID-ját.
     * @param roleId A szerepkör ID-ja.
     * @return Maga a User objektum (fluent interface).
     */
    public User setRoleId(Integer roleId) {
        this.roleId = roleId;
        return this;
    }

    /**
     * @return A felhasználó szerepkörének neve.
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Beállítja a felhasználó szerepkörének nevét.
     * @param roleName A szerepkör neve.
     * @return Maga a User objektum (fluent interface).
     */
    public User setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    /**
     * @return Igaz, ha a felhasználónak jelszót kell cserélnie a következő bejelentkezéskor.
     */
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /**
     * Beállítja a "jelszócsere kötelező" flag-et.
     * @param mustChangePassword true, ha a jelszócsere kötelező.
     * @return Maga a User objektum (fluent interface).
     */
    public User setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
        return this;
    }

    @Override
    public String toString() {
        return "User{id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", mustChangePassword=" + mustChangePassword +
                '}';
    }
}
