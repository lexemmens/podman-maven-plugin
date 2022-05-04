package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.enumeration.MatchType;
import org.apache.maven.plugin.logging.Log;

import java.util.EnumMap;

public class ImageTransformationService {

    private final Log log;
    private final SkopeoConfiguration skopeoConfiguration;

    private final EnumMap<MatchType, ImageTransformer> imageTransformers;

    public ImageTransformationService(Log log, SkopeoConfiguration skopeoConfiguration) {
        this.log = log;
        this.skopeoConfiguration = skopeoConfiguration;
        this.imageTransformers = new EnumMap<>(MatchType.class);

        String sourceImage = skopeoConfiguration.getCopy().getSourceImage();
        String destinationImage = skopeoConfiguration.getCopy().getDestinationImage();
        imageTransformers.put(MatchType.PARTIAL, new PartialMatchImageTransformer(sourceImage, destinationImage));
        imageTransformers.put(MatchType.REGEX, new RegexMatchImageTransformer(sourceImage, destinationImage));
        imageTransformers.put(MatchType.PRECISE, new PreciseMatchImageTransformer(sourceImage, destinationImage));
    }

    public ImageTransformer getImageTransformer() {
        return imageTransformers.get(skopeoConfiguration.getCopy().getMatchType());
    }

    public interface ImageTransformer {
        String transformImage(String imageName);
    }

    public abstract class AbstractImageTransformer implements ImageTransformer {

        protected final String sourceImage;
        protected final String destinationImage;

        AbstractImageTransformer(String sourceImage, String destinationImage) {
            this.sourceImage = sourceImage;
            this.destinationImage = destinationImage;
        }

    }

    public class RegexMatchImageTransformer extends AbstractImageTransformer {

        RegexMatchImageTransformer(String sourceImage, String destinationImage) {
            super(sourceImage, destinationImage);
        }

        @Override
        public String transformImage(String imageName) {
            String transformedImage = imageName.replaceAll(sourceImage, destinationImage);
            log.info("Transformed image from " + imageName + " to " + transformedImage);
            return transformedImage;
        }
    }

    public class PartialMatchImageTransformer extends AbstractImageTransformer {

        PartialMatchImageTransformer(String sourceImage, String destinationImage) {
            super(sourceImage, destinationImage);
        }

        @Override
        public String transformImage(String imageName) {
            String transformedImage = imageName;
            if(imageName.contains(sourceImage)) {
                transformedImage = imageName.replace(sourceImage, destinationImage);
            } else {
                log.warn("No transformation applied on image " + imageName + ". String " + sourceImage + " not found in image name.");
            }
            log.info("Transformed image from " + imageName + " to " + transformedImage);
            return transformedImage;
        }
    }

    public class PreciseMatchImageTransformer extends AbstractImageTransformer {

        PreciseMatchImageTransformer(String sourceImage, String destinationImage) {
            super(sourceImage, destinationImage);
        }

        @Override
        public String transformImage(String imageName) {
            String transformedImage = imageName;
            if(imageName.equals(sourceImage)) {
                transformedImage = destinationImage;
            } else {
                log.warn("No transformation applied on image " + imageName + ". Image name does not match " + sourceImage);
            }
            log.info("Transformed image from " + imageName + " to " + transformedImage);
            return transformedImage;
        }
    }



}
