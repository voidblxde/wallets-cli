package app.storage;

import app.domain.User;

import java.util.*;

public class UserRepository {
    private final Map<String, User> users = new HashMap<>();

    public Optional<User> find(String login) {
        if (login == null) return Optional.empty();
        return Optional.ofNullable(users.get(login));
    }

    public void upsert(User user) {
        users.put(user.getLogin(), user);
    }

    public boolean exists(String login) {
        return users.containsKey(login);
    }

    public List<User> getAll() {
        return new ArrayList<>(users.values());
    }
}
