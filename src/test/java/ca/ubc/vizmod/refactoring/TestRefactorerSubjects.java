package ca.ubc.vizmod.refactoring;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestRefactorerSubjects extends AbstractTestRefactorer {

    @Override
    protected String getTestsPath() {
        return "";
    }

    @Test
    public void testSubject11() {

        String subjectName = "subject-01/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"testimonial\"]/DIV/DIV[2]/DIV[1]/DIV[1]",
                "//*[@id=\"testimonial\"]/DIV/DIV[2]/DIV[1]/DIV[2]",
                "//*[@id=\"testimonial\"]/DIV/DIV[2]/DIV[2]/DIV[1]",
                "//*[@id=\"testimonial\"]/DIV/DIV[2]/DIV[2]/DIV[2]"
        );

        refactor(subjectName, "RC1", parentNodeXPaths, "refactored1");

    }

    @Test
    public void testSubject12() {

        String subjectName = "subject-01/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[1]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[2]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[3]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[4]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[5]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[6]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[7]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[8]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[9]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[10]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[11]",
                "//*[@id=\"portfolio\"]/DIV/DIV[3]/DIV/DIV[12]"
        );

        refactor(subjectName, "RC2", parentNodeXPaths, "refactored2");

    }

    @Test
    public void testSubject21() {

        String subjectName = "subject-02/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "/HTML/BODY/SECTION[3]/DIV/DIV/DIV[1]",
                "/HTML/BODY/SECTION[3]/DIV/DIV/DIV[2]",
                "/HTML/BODY/SECTION[3]/DIV/DIV/DIV[3]"
        );

       refactor(subjectName, "RC1", parentNodeXPaths, "refactored1");

    }

    @Test
    public void testSubject22() {

        String subjectName = "subject-02/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"feature\"]/DIV/DIV[2]/DIV[1]",
                "//*[@id=\"feature\"]/DIV/DIV[2]/DIV[2]",
                "//*[@id=\"feature\"]/DIV/DIV[2]/DIV[3]",
                "//*[@id=\"feature\"]/DIV/DIV[2]/DIV[4]",
                "//*[@id=\"feature\"]/DIV/DIV[2]/DIV[5]",
                "//*[@id=\"feature\"]/DIV/DIV[2]/DIV[6]"
        );

        refactor(subjectName, "RC2", parentNodeXPaths, "refactored2");

    }

    @Test
    public void testSubject23() {

        String subjectName = "subject-02/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"price\"]/DIV/DIV[2]/DIV[1]",
                "//*[@id=\"price\"]/DIV/DIV[2]/DIV[2]",
                "//*[@id=\"price\"]/DIV/DIV[2]/DIV[3]"
        );

        refactor(subjectName, "RC3", parentNodeXPaths, "refactored3");

    }

    @Test
    public void testSubject24() {

        String subjectName = "subject-02/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"blog\"]/DIV/DIV[2]/DIV[1]",
                "//*[@id=\"blog\"]/DIV/DIV[2]/DIV[2]",
                "//*[@id=\"blog\"]/DIV/DIV[2]/DIV[3]"
        );

        refactor(subjectName, "RC4", parentNodeXPaths, "refactored4");

    }

    @Test
    public void testSubject31() {

        String subjectName = "subject-03/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"project\"]/DIV/DIV[2]/DIV[1]",
                "//*[@id=\"project\"]/DIV/DIV[2]/DIV[2]",
                "//*[@id=\"project\"]/DIV/DIV[2]/DIV[3]"

        );

        refactor(subjectName, "RC1", parentNodeXPaths, "refactored1");

    }

    @Test
    public void testSubject32() {

        String subjectName = "subject-03/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "/HTML/BODY/SECTION[5]/DIV/DIV[2]/DIV[1]",
                "/HTML/BODY/SECTION[5]/DIV/DIV[2]/DIV[2]",
                "/HTML/BODY/SECTION[5]/DIV/DIV[2]/DIV[3]"
        );

        refactor(subjectName, "RC2", parentNodeXPaths, "refactored2");

    }

    @Test
    public void testSubject41() {

        String subjectName = "subject-04/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"about\"]/DIV/DIV/DIV[1]/DIV[2]",
                "//*[@id=\"about\"]/DIV/DIV/DIV[1]/DIV[3]",
                "//*[@id=\"about\"]/DIV/DIV/DIV[1]/DIV[4]"

        );

        refactor(subjectName, "RC1", parentNodeXPaths, "refactored1");

    }

    @Test
    public void testSubject42() {

        String subjectName = "subject-04/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"courses-wrapper\"]/DIV[1]/DIV[1]",
                "//*[@id=\"courses-wrapper\"]/DIV[1]/DIV[2]",
                "//*[@id=\"courses-wrapper\"]/DIV[1]/DIV[3]",
                "//*[@id=\"courses-wrapper\"]/DIV[1]/DIV[4]",
                "//*[@id=\"courses-wrapper\"]/DIV[2]/DIV[1]",
                "//*[@id=\"courses-wrapper\"]/DIV[2]/DIV[2]",
                "//*[@id=\"courses-wrapper\"]/DIV[2]/DIV[3]",
                "//*[@id=\"courses-wrapper\"]/DIV[2]/DIV[4]"

        );

        refactor(subjectName, "RC2", parentNodeXPaths, "refactored2");

    }

    @Test
    public void testSubject51() {

        String subjectName = "subject-05/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "/HTML/BODY/SECTION[2]/DIV/DIV[2]/DIV[1]/DIV/DIV",
                "/HTML/BODY/SECTION[2]/DIV/DIV[2]/DIV[2]/DIV[1]/DIV/DIV/DIV",
                "/HTML/BODY/SECTION[2]/DIV/DIV[2]/DIV[2]/DIV[2]/DIV/DIV/DIV",
                "/HTML/BODY/SECTION[2]/DIV/DIV[2]/DIV[3]/DIV[1]/DIV/DIV/DIV",
                "/HTML/BODY/SECTION[2]/DIV/DIV[2]/DIV[3]/DIV[2]/DIV/DIV/DIV"
        );

        refactor(subjectName, "RC1", parentNodeXPaths, "refactored1");

    }

    @Test
    public void testSubject52() {

        String subjectName = "subject-05/index.html";

        List<String> parentNodeXPaths = Arrays.asList(
                "/HTML/BODY/SECTION[3]/DIV/DIV[2]/DIV[1]",
                "/HTML/BODY/SECTION[3]/DIV/DIV[2]/DIV[2]",
                "/HTML/BODY/SECTION[3]/DIV/DIV[2]/DIV[3]"
        );

        refactor(subjectName, "RC2", parentNodeXPaths, "refactored2");

    }

}
