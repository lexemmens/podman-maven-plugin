package nl.lexemmens.podman.enumeration;

public enum PodmanCommand implements Command {

    PODMAN("podman"),
    LOGIN("login"),
    BUILD("build"),
    TAG("tag"),
    SAVE("save"),
    PUSH("push"),
    RMI("rmi");

    private String command;

    PodmanCommand(String command) {
        this.command = command;
    }

    @Override
    public String getCommand() {
        return command;
    }

}
