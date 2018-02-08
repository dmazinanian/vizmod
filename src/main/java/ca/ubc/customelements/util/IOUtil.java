package ca.ubc.customelements.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import ca.ubc.customelements.detection.DetectorTests;

public class IOUtil {
	
	public static String readResourceFileToString(String path) {
		try {
			ClassLoader classLoader = (new DetectorTests()).getClass().getClassLoader();
			File file = new File(classLoader.getResource(path).toURI().getPath());
			return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		} catch (URISyntaxException | IOException uriSyntaxException) {
			uriSyntaxException.printStackTrace();
			return "";
		}
	}
	
}
