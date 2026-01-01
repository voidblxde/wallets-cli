package app.domain;

public class User {
    private String login;
    private String passwordHash;
    private String salt;

    public User() {}

    public User(String login, String passwordHash, String salt) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }

    public void setLogin(String login) { this.login = login; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setSalt(String salt) { this.salt = salt; }
}
