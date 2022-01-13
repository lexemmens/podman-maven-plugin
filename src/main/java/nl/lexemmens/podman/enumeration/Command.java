package nl.lexemmens.podman.enumeration;

/**
 * Interface to group all command type enumerations to give {@link nl.lexemmens.podman.service.AbstractExecutorService}
 * some generic handles to work with and enforce type safety.
 */
public interface Command {

    /**
     * @return The command string represented by this command
     */
    String getCommand();
}
