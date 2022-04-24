package nl.lexemmens.podman.enumeration;

public enum BuildahCommand implements Command {

    BUILDAH("buildah"),
    UNSHARE("unshare");

    private String command;

    BuildahCommand(String command) {
        this.command = command;
    }

    @Override
    public String getCommand() {
        return command;
    }
}
