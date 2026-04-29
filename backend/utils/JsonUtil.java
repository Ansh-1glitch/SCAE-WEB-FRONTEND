// JsonUtil.java — JSON files read/write karne ka helper
// ye class data/ folder ke files se data padhti aur likhti hai

package utils;

import java.io.*;
import java.nio.file.*;

public class JsonUtil {

    // data folder ka path — yahan saare JSON files hain
    private static final String DATA_DIR = "data/";

    // file se JSON string read karo
    public static String readFile(String filename) throws IOException {
        Path path = Paths.get(DATA_DIR + filename);
        // agar file nahi hai toh empty array return karo
        if (!Files.exists(path)) {
            return "[]";
        }
        return new String(Files.readAllBytes(path));
    }

    // JSON string file mein write karo
    public static void writeFile(String filename, String jsonContent) throws IOException {
        Path path = Paths.get(DATA_DIR + filename);
        // parent directory create karo agar nahi hai
        Files.createDirectories(path.getParent());
        Files.write(path, jsonContent.getBytes());
    }
}
