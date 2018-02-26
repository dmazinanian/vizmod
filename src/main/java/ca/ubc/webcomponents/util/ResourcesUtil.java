package ca.ubc.webcomponents.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ResourcesUtil {

    public static final String GET_CSS_FOR_ELEMENT_JS = "css-for-element.js";

    public static final String CUSTOM_ELEMENT_REGISTER_JS = "custom-element-register.js";

    public static String readResourceFileToString(String path) {
        try {
            ClassLoader classLoader = IOUtil.class.getClassLoader();
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
