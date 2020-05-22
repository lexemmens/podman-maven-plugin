package nl.lexemmens.podman.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ImageConfigurationTest {

    @Test
    public void testEmptyImageConfiguration() throws MojoExecutionException {
        ImageConfiguration ic = new ImageConfiguration(null, null, null, false);
        Assertions.assertNull(ic.getRegistry());
        Assertions.assertFalse(ic.getImageHash().isPresent());
        Assertions.assertEquals(0, ic.getFullImageNames().size());
    }

//    @Test
    public void testTagWithoutRegistryCausesException() throws MojoExecutionException {
        ImageConfiguration ic = new ImageConfiguration(null, new String[]{"exampleTag"}, null, false);
        Assertions.assertNull(ic.getRegistry());
        Assertions.assertFalse(ic.getImageHash().isPresent());
        Assertions.assertThrows(MojoExecutionException.class, ic::getFullImageNames);
    }

    @Test
    public void testRepoInTagOnly() throws Exception
    {
        ImageConfiguration ic = new ImageConfiguration(null, new String[]{"registry.example.org:1234/repo/tag"}, "0.0.1", false);
        Assertions.assertNotNull(ic.getRegistry());
        Assertions.assertEquals("registry.example.org:1234", ic.getRegistry());
        Assertions.assertFalse(ic.getImageHash().isPresent());

        Assertions.assertDoesNotThrow(ic::getFullImageNames);
    }
}
