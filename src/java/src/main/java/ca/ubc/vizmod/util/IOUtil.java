package ca.ubc.vizmod.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class IOUtil {

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

    public static String readStringFromFile(String path) {
		try {
			return new String(Files.readAllBytes(new File(path).toPath()), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
}
