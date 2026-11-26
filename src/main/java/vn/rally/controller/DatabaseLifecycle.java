package vn.rally.controller;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.DriverManager;
import java.sql.SQLException;

@WebListener
public final class DatabaseLifecycle implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        AbandonedConnectionCleanupThread.checkedShutdown();
        ClassLoader loader = getClass().getClassLoader();
        DriverManager.drivers().filter(driver -> driver.getClass().getClassLoader() == loader).forEach(driver -> {
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException failure) {
                event.getServletContext().log("Cannot deregister JDBC driver", failure);
            }
        });
    }
}
