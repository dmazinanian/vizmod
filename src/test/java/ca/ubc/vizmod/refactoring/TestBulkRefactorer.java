package ca.ubc.vizmod.refactoring;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestBulkRefactorer extends AbstractTestRefactorer {

    @Override
    protected String getTestsPath() {
        return "";
    }

    @Test
    public void testSubject1() {

        String subjectName = "subject-01/index.html";
        String jsonFile = getTestsPath() + "results/refactoring_results_subject_1.json";

        List<List<String>> parentNodeXPaths = readJSONResultsFile(jsonFile);

        refactor(subjectName, parentNodeXPaths);

    }

    @Test
    public void testSubject2() {

        String subjectName = "subject-02/index.html";
        String jsonFile = getTestsPath() + "results/refactoring_results_subject_2.json";

        List<List<String>> parentNodeXPaths = readJSONResultsFile(jsonFile);

        refactor(subjectName, parentNodeXPaths);

    }

    @Test
    public void testSubject3() {

        String subjectName = "subject-03/index.html";
        String jsonFile = getTestsPath() + "results/refactoring_results_subject_3.json";

        List<List<String>> parentNodeXPaths = readJSONResultsFile(jsonFile);

        refactor(subjectName, parentNodeXPaths);

    }

    @Test
    public void testSubject4() {

        String subjectName = "subject-04/index.html";
        String jsonFile = getTestsPath() + "results/refactoring_results_subject_4.json";

        List<List<String>> parentNodeXPaths = readJSONResultsFile(jsonFile);

        refactor(subjectName, parentNodeXPaths);

    }

    @Test
    public void testSubject5() {

        String subjectName = "subject-05/index.html";
        String jsonFile = getTestsPath() + "results/refactoring_results_subject_5.json";

        List<List<String>> parentNodeXPaths = readJSONResultsFile(jsonFile);

        refactor(subjectName, parentNodeXPaths);

    }

    private List<List<String>> readJSONResultsFile(String jsonFile) {

        List<List<String>> parentNodeXPaths = new ArrayList<>();

        String json = readFileToString(jsonFile);

        List<List<String>> result =
                (List<List<String>>) new Gson().fromJson(json, LinkedTreeMap.class).get("refactoring_repetitions");

        for (List<String> l : result) {
            List<String> newList = new ArrayList<>();
            parentNodeXPaths.add(newList);
            for (String s : l) {
                Pattern pattern = Pattern.compile("/([a-z0-9]+)\\[");
                Matcher matcher = pattern.matcher(s);
                while (matcher.find()) {
                    String tagName = matcher.group(1);
                    s = s.replace(tagName, tagName.toUpperCase());
                }
                newList.add(s);
            }
        }

        return parentNodeXPaths;
    }

    private static String readFileToString(String jsonPath) {

        try {
            return FileUtils.readFileToString(new File(jsonPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";

    }

}
