package cl.bice.createStructure.service.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CreateStructureFolder {

    public static final String BASE_FOLDER = System.getenv("pathRelativeFolder") + "/features/" + System.getenv("nameSpace")+"/";
    public static final String BASE_FOLDER_ENV = System.getenv("pathRelativeFolder") + "/env/";

    public String getNameService(String name) {
        String[] splitName = name.split("-");
        StringBuilder newName = new StringBuilder();
        for (String string : splitName) {
            if (newName.length() == 0) {
                newName.append(string);
            } else {
                newName.append(string.substring(0, 1).toUpperCase()).append(string, 1, string.length());
            }
        }

        return newName.toString();
    }

    public void createStructureFolder(String folder) {
        createFolder(BASE_FOLDER + folder + File.separator + "structure");
        createFolder(BASE_FOLDER + folder + File.separator + "structure" + File.separator + "data");
        createFolder(BASE_FOLDER + folder + File.separator + "structure" + File.separator + "feature");
        createFolder(BASE_FOLDER + folder + File.separator + "structure" + File.separator + "helpers");
        createFolder(BASE_FOLDER + folder + File.separator + "structure" + File.separator + "request");
        createFolder(BASE_FOLDER + folder + File.separator + "structure" + File.separator + "response");
    }

    private void createFolder(String folder) {
        File theDir = new File(folder);
        if (!theDir.exists()){
            theDir.mkdir();
        }
    }

    public static void createFile(String pathFile, List<String> lines) throws IOException {
        File file = new File(BASE_FOLDER + pathFile);
        FileUtils.writeLines(file, lines);
    }

    public static void deleteBaseFolder(String folder) throws IOException {
        FileUtils.deleteDirectory(new File(BASE_FOLDER + folder));
    }

    public static void updateEnvFile(String nameFile, String tagService, String url) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(BASE_FOLDER_ENV + nameFile);
        Reader reader = Files.newBufferedReader(Paths.get(file.getPath()));
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> envLocal = gson.fromJson(reader, mapType);
        boolean existe = envLocal.containsKey("path" + tagService);

        if(!existe) {
            envLocal.put("path" + StringUtils.capitalize(tagService),url);
        }
        FileUtils.writeStringToFile(file, gson.toJson(envLocal), StandardCharsets.UTF_8);
    }

    private static List<String> converLines(String json) {
        String[] lines = json.split("/n");
        List<String> fileString = Arrays.asList(lines);
        fileString.replaceAll(s -> s.replace("/u0027", "'"));
        return fileString;
    }
}
