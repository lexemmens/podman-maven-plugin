package nl.lexemmens.podman.enumeration;

public enum BuildahCommand implements Command {

    BUILDAH("buildah"),
    UNSHARE("ushare");

    private String command;

    BuildahCommand(String command) {
        this.command = command;
    }

    @Override
    public String getCommand() {
        return command;
    }
}
