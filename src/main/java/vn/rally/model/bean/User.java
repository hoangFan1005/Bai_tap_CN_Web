package vn.rally.model.bean;

public final class User {
    private final long id;
    private final String name;
    private final String email;
    private final boolean admin;

    public User(long id, String name, String email, boolean admin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.admin = admin;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdmin() {
        return admin;
    }
}
