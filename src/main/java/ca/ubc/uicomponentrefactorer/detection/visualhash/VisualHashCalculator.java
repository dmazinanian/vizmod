package ca.ubc.uicomponentrefactorer.detection.visualhash;

import java.awt.image.BufferedImage;

public interface VisualHashCalculator {

    public String getVisualHash(BufferedImage bufferedImage);

}
