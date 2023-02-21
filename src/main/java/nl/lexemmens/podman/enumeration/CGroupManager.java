package nl.lexemmens.podman.enumeration;

/**
 * Enumeration that contains the possible values for the cgroup maanger that can be used
 */
public enum CGroupManager {
    SYSTEMD("systemd"),
    CGROUPFS("cgroupfs");

    private final String cgroupManager;

    /**
     * Constructs a new instance of this enumeration
     *
     * @param cgroupManager The cgroupManager to set
     */
    CGroupManager(String cgroupManager) {
        this.cgroupManager = cgroupManager;
    }

    /**
     * Returns the cgroupManager corresponding to the selected value
     *
     * @return The corresponding cgroupManager.
     */
    public String getCgroupManager() {
        return cgroupManager;
    }
}
