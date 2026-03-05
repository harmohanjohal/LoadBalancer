package com.mycompany.javafxapplication1;

/**
 * Singleton that holds the currently authenticated {@link User}
 * for the lifetime of the application session, with idle timeout support.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    private static final long SESSION_TIMEOUT_MS = 15 * 60 * 1000L; // 15 minutes

    private volatile User loggedInUser;
    private volatile long lastActivityTime;

    private SessionManager() {
    }

    /** Returns the singleton instance. */
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Sets the user for the current session. */
    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        this.lastActivityTime = System.currentTimeMillis();
    }

    /** Returns the currently logged-in user, or {@code null} if none or session expired. */
    public User getLoggedInUser() {
        if (loggedInUser != null && isSessionExpired()) {
            clearSession();
            return null;
        }
        return loggedInUser;
    }

    /** Records user activity to reset the idle timer. */
    public void touch() {
        if (loggedInUser != null) {
            this.lastActivityTime = System.currentTimeMillis();
        }
    }

    /** Clears the current session, logging out the user. */
    public void clearSession() {
        this.loggedInUser = null;
        this.lastActivityTime = 0;
    }

    /** Returns {@code true} if a user is currently logged in and session is not expired. */
    public boolean isLoggedIn() {
        return getLoggedInUser() != null;
    }

    private boolean isSessionExpired() {
        return (System.currentTimeMillis() - lastActivityTime) > SESSION_TIMEOUT_MS;
    }
}
