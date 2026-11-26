package vn.rally.model.bo;

import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import vn.rally.model.bean.User;
import vn.rally.model.dao.UserDAO;

public final class AuthBO {
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final String UNUSED_HASH = Passwords.hash(UUID.randomUUID().toString());
    private final UserDAO users = new UserDAO();

    public User authenticate(String email, String password) throws SQLException {
        email = email(email);
        if (!validEmail(email) || !validPassword(password)) {
            return null;
        }
        var account = users.findByEmail(email);
        boolean verified = Passwords.matches(password, account == null ? UNUSED_HASH : account.passwordHash());
        return verified && account != null ? account.user() : null;
    }

    /** Returns a form error, or null when the new customer is stored. */
    public String register(String name, String email, String password) throws SQLException {
        name = name == null ? "" : name.strip();
        email = email(email);
        if (name.isBlank() || name.length() > 120) {
            return "Nhập họ tên từ 1 đến 120 ký tự.";
        }
        if (!validEmail(email)) {
            return "Nhập địa chỉ email hợp lệ.";
        }
        if (!validPassword(password)) {
            return "Mật khẩu cần có từ 8 đến 128 ký tự.";
        }
        if (!users.create(name, email, Passwords.hash(password))) {
            return "Email này đã được đăng ký.";
        }
        return null;
    }

    private static String email(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean validEmail(String value) {
        return value.length() <= 254 && EMAIL.matcher(value).matches();
    }

    private static boolean validPassword(String value) {
        return value != null && value.length() >= 8 && value.length() <= 128;
    }
}
