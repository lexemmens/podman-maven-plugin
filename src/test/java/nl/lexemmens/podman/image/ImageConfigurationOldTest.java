package nl.lexemmens.podman.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImageConfigurationOldTest {

//    @Test
//    void testEmptyImageConfiguration() throws MojoExecutionException {
//        // Tags cannot be empty. If tagging should not be done it should be skipped by specifying the appropriate parameter
//        // For saving and pushing everything is required
//        ImageConfigurationOld ic = new ImageConfigurationOld(null, null, null, false);
//        Assertions.assertNull(ic.getTargetRegistry());
//        Assertions.assertFalse(ic.getImageHash().isPresent());
//        Assertions.assertThrows(MojoExecutionException.class, ic::getFullImageNames);
//    }
//
//    @Test
//    void testTagWithoutVersionCausesException() throws MojoExecutionException {
//        // No version is specified as well as createImageTaggedLatest is set to false. Should fail because no version at all is available
//
//        ImageConfigurationOld ic = new ImageConfigurationOld(null, new String[]{"exampleTag"}, null, false);
//        Assertions.assertEquals("exampleTag", ic.getTargetRegistry()); // Registry is allowed to be part of the tag
//        Assertions.assertFalse(ic.getImageHash().isPresent());
//        Assertions.assertThrows(MojoExecutionException.class, ic::getFullImageNames);
//    }
//
//    @Test
//    void testTagWithLatestTagCausesNoException() throws MojoExecutionException {
//        // No version is specified as well as createImageTaggedLatest is set to false. Should fail because no version at all is available
//
//        ImageConfigurationOld ic = new ImageConfigurationOld(null, new String[]{"exampleTag"}, null, true);
//        Assertions.assertEquals("exampleTag", ic.getTargetRegistry()); // Registry is allowed to be part of the tag
//        Assertions.assertFalse(ic.getImageHash().isPresent());
//        Assertions.assertDoesNotThrow(ic::getFullImageNames);
//    }
//
//    @Test
//    void testTagWithVersionAndNoLatestTagCausesNoException() throws MojoExecutionException {
//        // No version is specified as well as createImageTaggedLatest is set to false. Should fail because no version at all is available
//
//        ImageConfigurationOld ic = new ImageConfigurationOld(null, new String[]{"exampleTag"}, "1.0.0", false);
//        Assertions.assertEquals("exampleTag", ic.getTargetRegistry()); // Registry is allowed to be part of the tag
//        Assertions.assertFalse(ic.getImageHash().isPresent());
//        Assertions.assertDoesNotThrow(ic::getFullImageNames);
//    }
//
//    @Test
//    void testRepoInTagOnly() throws Exception {
//        ImageConfigurationOld ic = new ImageConfigurationOld(null, new String[]{"registry.example.org:1234/repo/tag"}, "0.0.1", false);
//        Assertions.assertNotNull(ic.getTargetRegistry());
//        Assertions.assertEquals("registry.example.org:1234", ic.getTargetRegistry());
//        Assertions.assertFalse(ic.getImageHash().isPresent());
//
//        Assertions.assertDoesNotThrow(ic::getFullImageNames);
//    }
//
//    @Test
//    void testTargetRegistryNullWithInvalidTag() throws Exception {
//        ImageConfigurationOld ic = new ImageConfigurationOld(null, new String[]{"file:///home/user/folder:1234/repo/tag"}, "0.0.1", false);
//        Assertions.assertNotNull(ic.getTargetRegistry());
//    }
}
