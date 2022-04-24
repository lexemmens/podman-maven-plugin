package nl.lexemmens.podman.enumeration;

public enum PodmanCommand implements Command {

    PODMAN("podman"),
    LOGIN("login"),
    BUILD("build"),
    TAG("tag", false),
    SAVE("save", false),
    PUSH("push"),
    RMI("rmi", false),
    VERSION("version", false, false);

    private final String command;
    private final boolean tlsSupported;
    private final boolean runRootSupported;

    PodmanCommand(String command) {
        this(command, true, true);
    }

    PodmanCommand(String command, boolean tlsSupported) {
        this(command, tlsSupported, true);
    }

    PodmanCommand(String command, boolean tlsSupported, boolean runRootSupported) {
        this.command = command;
        this.tlsSupported = tlsSupported;
        this.runRootSupported = runRootSupported;
    }

    @Override
    public String getCommand() {
        return command;
    }

    public boolean isTlsSupported() {
        return tlsSupported;
    }

    public boolean isRunRootSupported() {
        return runRootSupported;
    }
}
