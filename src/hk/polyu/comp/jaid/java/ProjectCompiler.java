package hk.polyu.comp.jaid.java;

import hk.polyu.comp.jaid.fixer.config.FixerOutput;
import hk.polyu.comp.jaid.util.FileUtil;

import javax.tools.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Max PEI.
 */
public class ProjectCompiler {
    private JavaProject javaProject;
    private List<String> commonOptions;

    private static JavaCompiler sharedCompiler;
    private static MutableDiagnosticCollector<JavaFileObject> sharedDiagnostics;
    private static StandardJavaFileManager sharedFileManager;
    private static List<String> sharedOptions;

    public ProjectCompiler(JavaProject javaProject) {
        this.javaProject = javaProject;

        prepareCommonOptions();
    }

    public static MutableDiagnosticCollector<JavaFileObject> getSharedDiagnostics() {
        return sharedDiagnostics;
    }

    public static final int MAX_ERROR_NUMBER = Integer.MAX_VALUE;

    private void prepareCommonOptions() {
        commonOptions = new LinkedList<>();

        // Force the compiler to report all errors
        commonOptions.add("-Xmaxerrs");
        commonOptions.add(MAX_ERROR_NUMBER + "");

        String projectSpecificOptions = javaProject.getSpecificCompilationOptions();
        if (projectSpecificOptions != null && !projectSpecificOptions.isEmpty()) {
            commonOptions.add(projectSpecificOptions);
        }

        if (javaProject.getEncoding() != null && javaProject.getEncoding().length() > 0) {
            commonOptions.add("-encoding");
            commonOptions.add(javaProject.getEncoding());
        }

        if (javaProject.getTargetJavaVersion() != null && javaProject.getTargetJavaVersion().length() > 0) {
            commonOptions.add("-source");
            commonOptions.add(javaProject.getTargetJavaVersion());
            commonOptions.add("-target");
            commonOptions.add(javaProject.getTargetJavaVersion());
        }
        commonOptions.add("-g");
    }

    private void safeSetClassPath(List<Path> classpaths) {
        List<File> libraries = classpaths.stream().map(x -> x.toFile()).collect(Collectors.toList());
        try {
            sharedFileManager.setLocation(StandardLocation.CLASS_PATH, libraries);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid class paths.");
        }
    }

    private void safeSetClassOutput(Path outputDir) {
        try {
            sharedFileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputDir.toFile()));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid output dir.");
        }
    }

    public DiagnosticCollector<JavaFileObject> compilePatch(){
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        List<String> specificOptions = new LinkedList<>();
        specificOptions.addAll(commonOptions);
        specificOptions.add("-cp");
        specificOptions.add(javaProject.getClasspathStr());
        specificOptions.add("-d");
        specificOptions.add(javaProject.getPatchOutputDir().toString());
//        List<File> files = javaProject.getPatchSourceFiles().stream().map(x -> x.toFile()).collect(Collectors.toList());

        File file = new File("C:\\Users\\HDULAB601\\Desktop\\jaid\\jaid\\example\\af_test\\src\\patch\\java\\af_test\\MyList.java");
        List<File> files = new ArrayList<>();
        files.add(file);

        Iterable<? extends JavaFileObject> sourceFileObjects = fileManager.getJavaFileObjectsFromFiles(files);
        compiler.getTask(null, fileManager, diagnostics, specificOptions, null, sourceFileObjects).call();
        return diagnostics;
    }
    public DiagnosticCollector<JavaFileObject> compileOriginalSource() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        List<String> specificOptions = new LinkedList<>();
        specificOptions.addAll(commonOptions);
        specificOptions.add("-cp");
        specificOptions.add(javaProject.getClasspathStr());
        specificOptions.add("-d");
        specificOptions.add(javaProject.getOutputDir().toString());
        List<File> files = javaProject.getSourceFiles().stream().map(x -> x.toFile()).collect(Collectors.toList());
        Iterable<? extends JavaFileObject> sourceFileObjects = fileManager.getJavaFileObjectsFromFiles(files);

        compiler.getTask(null, fileManager, diagnostics, specificOptions, null, sourceFileObjects).call();
        return diagnostics;
    }

    public DiagnosticCollector<JavaFileObject> compileTestSource() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        List<String> specificOptions = new LinkedList<>();
        specificOptions.addAll(commonOptions);
        specificOptions.add("-cp");
        specificOptions.add(javaProject.getClasspathStr());
        specificOptions.add("-d");
        specificOptions.add(javaProject.getTestOutputDir().toString());
        List<File> files = javaProject.getTestSourceFiles().stream().map(x -> x.toFile()).collect(Collectors.toList());

        System.out.println(specificOptions.toString());
        Iterable<? extends JavaFileObject> sourceFileObjects = fileManager.getJavaFileObjectsFromFiles(files);
        compiler.getTask(null, fileManager, diagnostics, specificOptions, null, sourceFileObjects).call();
        return diagnostics;
    }
    //新添加的方法，用来编译Evosiute的测试用例
    public DiagnosticCollector<JavaFileObject> compileNewTestSource(){
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        List<String> specificOptions = new LinkedList<>();
        specificOptions.addAll(commonOptions);
        specificOptions.add("-cp");
        specificOptions.add(javaProject.getClasspathStr() + ":" + javaProject.getStateSnapshotFive().jarPath);
        //注意win使用“；”来连接jar包，linux使用“：”来连接jar包的
        //        System.out.println(javaProject.getClasspathStr() + ":" + javaProject.getStateSnapshotFive().jarPath);
        specificOptions.add("-d");
        specificOptions.add(javaProject.getTestOutputDir().toString());

        Iterator<String> iterator = specificOptions.iterator();
        while (iterator.hasNext()){
            String str = iterator.next();
            if(str.equals("-source") || str.equals("-target")){
                iterator.remove();
                iterator.next();
                iterator.remove();
            }
        }
        List<File> files = javaProject.getStateSnapshotFive().getEvosuiteTSource().stream().map(x -> x.toFile()).collect(Collectors.toList());
//        File file = new File("/home/zxdemm/Arja/jaid/example1/af_test/out/evosuite-tests/Alphabet_ESTest.java");
//        List<File> files = new ArrayList<>();
//        files.add(file);
//        File file1 = new File("/home/zxdemm/Arja/jaid/example1/af_test/out/evosuite-tests/Alphabet_ESTest_scaffolding.java");
//        files.add(file1);
        Iterable<? extends JavaFileObject> sourceFileObjects = fileManager.getJavaFileObjectsFromFiles(files);
        compiler.getTask(null, fileManager, diagnostics, specificOptions, null, sourceFileObjects).call();
        return diagnostics;
    }
    public void compileFormattedCTF() {
        Path ctf = javaProject.getFormattedSourceFileToFix();
        incrementalCompile(ctf);
    }

    public void compileFixCandidatesInBatch() {
        Path p = javaProject.getSourceFileWithAllFixes();
        incrementalCompile(p);
    }

    private void prepareSharedObjects() {
        if (sharedCompiler == null) {
            sharedCompiler = ToolProvider.getSystemJavaCompiler();
            sharedDiagnostics = new MutableDiagnosticCollector<>();
            sharedFileManager = sharedCompiler.getStandardFileManager(sharedDiagnostics, null, null);

            sharedOptions = new LinkedList<>();
            sharedOptions.addAll(commonOptions);
            sharedOptions.add("-cp");
            sharedOptions.add(javaProject.getClasspathForFixingStr());
            sharedOptions.add("-d");
            sharedOptions.add(FixerOutput.getTempDestDirPath().toString());
        } else {
            sharedDiagnostics.clear();
        }
    }

    public void incrementalCompile(Path sourceFilePath) {
        prepareSharedObjects();

        Iterable<? extends JavaFileObject> sourceFiles = sharedFileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFilePath.toFile()));

        sharedCompiler.getTask(null, sharedFileManager, sharedDiagnostics, sharedOptions, null, sourceFiles).call();
    }

    private List<Path> getOriginalSourceFiles() {
        List<Path> result = new LinkedList<>();
        javaProject.getSourceDirs().stream().forEach(x -> result.addAll(FileUtil.javaFiles(x, true, false)));
        return result;
    }

    private List<Path> getTestSourceFiles() {
        List<Path> result = new LinkedList<>();
        javaProject.getTestSourceDirs().stream().forEach(x -> result.addAll(FileUtil.javaFiles(x, true, false)));
        return result;
    }

}
