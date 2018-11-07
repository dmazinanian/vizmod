package ca.ubc.vizmod.detection.visualhash;

import java.awt.image.BufferedImage;

public interface VisualHashCalculator {

    public String getVisualHash(BufferedImage bufferedImage);

}
