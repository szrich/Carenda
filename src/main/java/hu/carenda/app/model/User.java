package hu.carenda.app.model;

/**
 * Egyszerű User modell az authentikációhoz és jogosultsághoz.
 */
public class User {

    private int id;
    private String username;
    private String passwordHash;
    private String fullName;

    // Szerep információk (opcionálisak, lehetnek null-ok)
    private Integer roleId;
    private String roleName;

    // ÚJ: flag, hogy a felhasználónak kötelező-e jelszót cserélnie
    private boolean mustChangePassword;

    public int getId() {
        return id;
    }

    public User setId(int id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public User setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public User setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public User setRoleId(Integer roleId) {
        this.roleId = roleId;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public User setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    // --- ÚJ mező: mustChangePassword ---
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

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
