package nl.lexemmens.podman.enumeration;

public enum BuildahCommand {

    BUILDAH("buildah"),
    UNSHARE("unshare");

    private final String command;

    BuildahCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
