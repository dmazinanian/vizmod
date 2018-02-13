package ca.ubc.customelements.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class IOUtil {

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

	public static void writeStringToFile(String string, String path) {
		try {
			File file = new File(path);
			OutputStreamWriter writer = 
					new OutputStreamWriter(new FileOutputStream(file), Charset.defaultCharset());
			writer.write(string);
			writer.flush();
			writer.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

}
