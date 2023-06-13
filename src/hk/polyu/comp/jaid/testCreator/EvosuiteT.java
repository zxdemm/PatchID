package hk.polyu.comp.jaid.testCreator;

import com.sun.jdi.Field;
import hk.polyu.comp.jaid.java.JavaProject;
import hk.polyu.comp.jaid.test.TestExecutionResult;
import hk.polyu.comp.jaid.tester.TestRequest;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EvosuiteT {
    JavaProject project;
    String bugClassName;//MyList
    String testClassName;//MyList_ESTest
//    String testfullyQualifiedClassName = "af_test.MyList_ESTest";//af_test.MyList_ESTest
    String testfullyQualifiedClassName;

    List<TestExecutionResult> testExecutionResults;
    //没有过滤的测试用例
    List<TestRequest> newTestRequest;
    //覆盖fixMe的测试用例
    List<TestRequest> testsCoveredBugMethod;
    List<TestExecutionResult> testExecutionResultsCoveredBug;
    //满足三元组表达式的测试用例
    List<TestRequest> testsSatisfiedSnapshots;



    //举例子罢了
    public void createTests() throws IOException {
        //javacTestSource();
        //为了搞的快一点，首先把RunCmd();setPathToNewTest();editNewTest();
        RunCmd();
        setPathToNewTest();
        editNewTest();
    }
    public void editNewTest() throws IOException {
        File file = project.getStateSnapshotFive().getEvosiuteTFilePath().toFile();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        // 一行一行的读
        StringBuilder sb = new StringBuilder();

        while ((line = reader.readLine()) != null) {

            if(line.startsWith("@RunWith(EvoRunner.class)")){
                sb.append("//" + line);
            }
            else{
                sb.append(line);
            }
            sb.append("\r\n");
        }
        reader.close();

        //写回去
        RandomAccessFile mm = new RandomAccessFile(file, "rw");
        mm.writeBytes(sb.toString());
        mm.close();
    }
    public void javacTestSource(){
        String cd = project.getSourceDirs().get(0).toString();
        Process p;
        try{
            p = Runtime.getRuntime().exec("javac cd");
            InputStream inputStream = p.getInputStream();
//            InputStream inputStream = p.getErrorStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line = "";
            while ((line = br.readLine()) != null){
                System.out.println(line);
            }
            System.out.println("over 编译");
            int a = p.waitFor();
            System.out.println("p : " + a);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void RunCmd() throws IOException {
        String cd = project.getOutputDir().toString();
        String className = project.getMethodToMonitor().getFullQualifiedClassName();
        String[] strings = className.split("\\.");

        this.bugClassName = strings[strings.length - 1];
        this.testfullyQualifiedClassName = className + "_ESTest";
        String opt1 = "cd " + project.getOutputDir().toString();
        String opt2 = "java -jar " + project.getStateSnapshotFive().evosuiteJar + " -projectCP ./ " + "-class " + className;
        this.testClassName = bugClassName + "_ESTest";
        Process p;
        System.out.println(opt1);
        System.out.println(opt2);
        try{
            String[] cmds = new String[] {
                    "/bin/sh",
                    "-c",
                    opt1 + "&&" + opt2};
            p = Runtime.getRuntime().exec(cmds);
            InputStream inputStream = p.getInputStream();
//            InputStream inputStream = p.getErrorStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line = "";
            while ((line = br.readLine()) != null){
                System.out.println(line);
            }
            System.out.println("测试用例生成结束");
            int a = p.waitFor();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void getTestFile(List<Path> lists, File file){
        File[] files = file.listFiles();
        for(File file1 : files){
            if(file1.isFile()){
                if(file1.getName().equals(testClassName + ".java")){
                    project.getStateSnapshotFive().setEvosiuteTFilePath(file1.toPath());
                }
                lists.add(file1.toPath());
            }
            if(file1.isDirectory())getTestFile(lists, file1);
        }
    }
    public void setPathToNewTest(){
        //设置新测试用例java文件的路径
        System.out.println("设置新测试用例java文件的路径");
        //得到package.classname
        String fullQualifiedClassName = project.getMethodToMonitor().getFullQualifiedClassName();
        List<Path> lists = new ArrayList<>();
        File fileEvosiute = new File(project.getOutputDir().toString() + "/evosuite-tests");
        File[] files =fileEvosiute.listFiles();
        for (File file : files){
            getTestFile(lists, file);
        }
        project.getStateSnapshotFive().setEvosuiteTSources(lists);

    }
    public JavaProject getProject() {
        return project;
    }

    public void setProject(JavaProject project) {
        this.project = project;
    }

    public List<TestExecutionResult> getTestExecutionResults() {
        return testExecutionResults;
    }

    public void setTestExecutionResults(List<TestExecutionResult> testExecutionResults) {
        this.testExecutionResults = testExecutionResults;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public void setTestClassName(String testClassName) {
        this.testClassName = testClassName;
    }

    public String getBugClassName() {
        return bugClassName;
    }

    public void setBugClassName(String bugClassName) {
        this.bugClassName = bugClassName;
    }

    public List<TestRequest> getNewTestRequest() {
        return newTestRequest;
    }

    public void setNewTestRequest(List<TestRequest> newTestRequest) {
        this.newTestRequest = newTestRequest;
    }

    public String getTestfullyQualifiedClassName() {
        return testfullyQualifiedClassName;
    }

    public List<TestRequest> getTestsCoveredBugMethod() {
        return testsCoveredBugMethod;
    }

    public void setTestsCoveredBugMethod(List<TestRequest> testsCoveredBugMethod) {
        this.testsCoveredBugMethod = testsCoveredBugMethod;
    }

    public List<TestRequest> getTestsSatisfiedSnapshots() {
        return testsSatisfiedSnapshots;
    }

    public void setTestsSatisfiedSnapshots(List<TestRequest> testsSatisfiedSnapshots) {
        this.testsSatisfiedSnapshots = testsSatisfiedSnapshots;
    }

    public void setTestfullyQualifiedClassName(String testfullyQualifiedClassName) {
        this.testfullyQualifiedClassName = testfullyQualifiedClassName;
    }

    public List<TestExecutionResult> getTestExecutionResultsCoveredBug() {
        return testExecutionResultsCoveredBug;
    }

    public void setTestExecutionResultsCoveredBug(List<TestExecutionResult> testExecutionResultsCoveredBug) {
        this.testExecutionResultsCoveredBug = testExecutionResultsCoveredBug;
    }
}
