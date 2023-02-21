package nl.lexemmens.podman.enumeration;

/**
 * Enumeration that contains the possible values for the cgroup maanger that can be used
 */
public enum CGroupManager {
    SYSTEMD("--cgroup-manager=systemd"),
    CGROUPFS("--cgroup-manager=cgroupfs");

    private final String command;

    /**
     * Constructs a new instance of this enumeration
     *
     * @param command The cgroupManager command to use
     */
    CGroupManager(String command) {
        this.command = command;
    }

    /**
     * Returns the cgroupManager corresponding to the selected value
     *
     * @return The corresponding cgroupManager.
     */
    public String getCommand() {
        return command;
    }
}
