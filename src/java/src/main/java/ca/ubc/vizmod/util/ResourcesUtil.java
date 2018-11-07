package ca.ubc.vizmod.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ResourcesUtil {

    public static final String GET_CSS_FOR_ELEMENT_JS = "ca/ubc/vizmod/adaptingstrategies/css-for-element.js";

    public static String readResourceFileToString(String path) {
        try {
            ClassLoader classLoader = ResourcesUtil.class.getClassLoader();
            File file = new File(classLoader.getResource(path).toURI().getPath());
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (URISyntaxException uriSyntaxException) {
            uriSyntaxException.printStackTrace();
            return "";
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return "";
        }
    }
}
