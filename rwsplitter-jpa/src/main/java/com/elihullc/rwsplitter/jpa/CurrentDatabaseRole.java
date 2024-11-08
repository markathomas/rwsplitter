package com.elihullc.rwsplitter.jpa;

/**
 * Utility class for holding the inheritable thread-local value for the current {@link DatabaseRole}.  The initial value of the
 * thread-local is {@link DatabaseRole#WRITER}.
 */
public final class CurrentDatabaseRole {

    private static final ThreadLocal<DatabaseRole> CURRENT_ROLE = ThreadLocal.withInitial(() -> DatabaseRole.WRITER);

    private CurrentDatabaseRole() {
    }

    /**
     * Returns the {@link DatabaseRole} for the current thread
     * @return current {@link DatabaseRole}
     */
    public static DatabaseRole getCurrentRole() {
        return CURRENT_ROLE.get();
    }

    /**
     * Sets the {@link DatabaseRole} for the current thread
     * @param role new {@link DatabaseRole} for the current thread
     */
    public static void setCurrentRole(DatabaseRole role) {
        CURRENT_ROLE.set(role);
    }

    /**
     * Resets the {@link DatabaseRole} for the current thread to {@link DatabaseRole#WRITER}
     */
    public static void resetCurrentRole() {
        CURRENT_ROLE.set(DatabaseRole.WRITER);
    }
}
