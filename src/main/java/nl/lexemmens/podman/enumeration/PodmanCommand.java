package nl.lexemmens.podman.enumeration;

public enum PodmanCommand {

    PODMAN("podman"),
    LOGIN("login"),
    BUILD("build"),
    TAG("tag"),
    SAVE("save"),
    PUSH("push"),
    RMI("rmi"),
    VERSION("version");

    private final String command;

    PodmanCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

}
