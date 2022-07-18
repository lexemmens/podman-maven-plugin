package nl.lexemmens.podman.enumeration;

public enum SkopeoCommand {

    SKOPEO("skopeo"),
    COPY("copy");

    private final String command;

    SkopeoCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
