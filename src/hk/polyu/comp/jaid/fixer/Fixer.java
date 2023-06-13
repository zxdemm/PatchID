package hk.polyu.comp.jaid.fixer;

import ch.qos.logback.classic.Level;
import hk.polyu.comp.jaid.Detection.ClassToFixPreprocessor_Patch;
import hk.polyu.comp.jaid.Detection.Detector;
import hk.polyu.comp.jaid.fixaction.FixAction;
import hk.polyu.comp.jaid.fixaction.FixActionBuilder;
import hk.polyu.comp.jaid.fixaction.Snippet;
import hk.polyu.comp.jaid.fixaction.SnippetBuilder;
import hk.polyu.comp.jaid.fixer.config.Config;
import hk.polyu.comp.jaid.fixer.config.FailureHandling;
import hk.polyu.comp.jaid.fixer.log.LoggingService;
import hk.polyu.comp.jaid.java.ClassToFixPreprocessor;
import hk.polyu.comp.jaid.java.JavaProject;
import hk.polyu.comp.jaid.java.ProjectCompiler;
import hk.polyu.comp.jaid.monitor.LineLocation;
import hk.polyu.comp.jaid.monitor.jdi.debugger.TestByLocationSelector;
import hk.polyu.comp.jaid.monitor.snapshot.StateSnapshot;
import hk.polyu.comp.jaid.monitor.snapshot.StateSnapshotFive;
import hk.polyu.comp.jaid.monitor.state.DebuggerEvaluationResult;
import hk.polyu.comp.jaid.monitor.state.ProgramState;
import hk.polyu.comp.jaid.monitor.suspiciouness.AbsSuspiciousnessAlgorithm;
import hk.polyu.comp.jaid.test.TestExecutionResult;
import hk.polyu.comp.jaid.testCreator.EvosuiteT;
import hk.polyu.comp.jaid.tester.TestRequest;
import org.eclipse.jdt.core.dom.Statement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static hk.polyu.comp.jaid.fixer.config.FixerOutput.LogFile.*;
import static hk.polyu.comp.jaid.fixer.log.LoggingService.*;
import static hk.polyu.comp.jaid.util.LogUtil.*;

public class Fixer {
    Config config;
    JavaProject javaProject;

    public void execute() throws Exception {

        // Initialization
        config = Session.getSession().getConfig();
        javaProject = config.getJavaProject();

        ClassToFixPreprocessor preprocessor = new ClassToFixPreprocessor(javaProject, config);
        //Rewrite method to fix，反正这一步就是写jaid_output.formatted_src.af_test.MyList.java这个文件路径到formattedSourceFileToFix,以及写
        //project中的这个sourceFileWithAllFixes变量
        preprocessor.preprocess();

        //这个方法是将整个关于源文件的ast树加载下来
        javaProject.registerMethodToMonitor(config);
        javaProject.initMethodToMonitor();
        javaProject.compile();
        // Monitoring
        ProgramMonitor programMonitor = new ProgramMonitor(config);
        Map<AbsSuspiciousnessAlgorithm.SbflAlgorithm, List<StateSnapshot>> stateSnapshots = programMonitor.execute();
        //我这里写出获取最大可疑度的三元组
//        StateSnapshot stateSnapshotf = stateSnapshots.get(AbsSuspiciousnessAlgorithm.SbflAlgorithm.AutoFix).get(0);
//        System.out.println(stateSnapshotf.getLocation().getStatement());
//        System.out.println(stateSnapshotf.getSnapshotExpression());



       // System.out.println("开始第二阶段");
        startNewTests(javaProject,stateSnapshots,programMonitor);


//        StateSnapshot stateSnapshotf = stateSnapshots.get(AbsSuspiciousnessAlgorithm.SbflAlgorithm.AutoFix).get(0);
//        for(StateSnapshot stateSnapshot : stateSnapshots.get(AbsSuspiciousnessAlgorithm.SbflAlgorithm.AutoFix)){
//            System.out.println(stateSnapshot);
//        }
//        System.out.println(stateSnapshotf.getLocation().getStatement());
//        System.out.println(stateSnapshotf.getSnapshotExpression());
        System.out.println("开始第三阶段");
        DetectionWork();

//




//        if (shouldLogDebug()) {
//            for (AbsSuspiciousnessAlgorithm.SbflAlgorithm sbflAlgorithm : stateSnapshots.keySet()) {
//                LoggingService.debugFileOnly("Generated snapshots - " + sbflAlgorithm.toString() + " :: "
//                        + stateSnapshots.get(sbflAlgorithm).size(), SUSPICIOUS_STATE_SNAPSHOT);
//                int rank = 0;
//                for (StateSnapshot stateSnapshot : stateSnapshots.get(sbflAlgorithm)) {
//                    LoggingService.debugFileOnly(rank++ + ":: " + stateSnapshot.toString(), SUSPICIOUS_STATE_SNAPSHOT);
//                }
//            }
//        }
/*
这个下面的都是我注释掉了，下面都是修复操作
 */
        // Generate fix action for each suspicious snapshot list
//        addFileLogger(CANDIDATE_ID_FOR_SBFL, Level.DEBUG);
//        Map<Long, FixAction> allFixes = new LinkedHashMap<>();
//        Map<AbsSuspiciousnessAlgorithm.SbflAlgorithm, List<Long>> fixActionMaps = new HashMap<>();
//        for (AbsSuspiciousnessAlgorithm.SbflAlgorithm sbflAlgorithm : stateSnapshots.keySet()) {
//            //Fix generation
//            List<FixAction> fixes = generateFixActions(stateSnapshots.get(sbflAlgorithm)).values().stream()
//                    .flatMap(Set::stream).collect(Collectors.toCollection(LinkedList::new));
//            List<Long> fixIdList = new ArrayList<>();
//            removeAllIllformedFixActions(fixes);//each fix get its id after this
//            fixes.sort((o1, o2) -> Double.compare(o2.getStateSnapshot().getSuspiciousness(sbflAlgorithm),
//                    o1.getStateSnapshot().getSuspiciousness(sbflAlgorithm)));
//            for (FixAction fix : fixes) {
//                if (allFixes.keySet().contains(fix.getFixId()))
//                    allFixes.get(fix.getFixId()).updateSeedForDuplicated(fix);
//                else
//                    allFixes.put(fix.getFixId(), fix);
//                fixIdList.add(fix.getFixId());
//            }
//            fixActionMaps.put(sbflAlgorithm, fixIdList);
//            LoggingService.infoAll("Generated fixactions - " + sbflAlgorithm.toString() + " ::" + fixIdList.size());
//            fixIdList.forEach(x -> LoggingService.debugFileOnly("fixId :: " + x, CANDIDATE_ID_FOR_SBFL));
//        }
//        removeExtraLogger(CANDIDATE_ID_FOR_SBFL);
//
//
////         Validations
//        List<FixAction> allFixActions = new LinkedList<>(allFixes.values());
//        if (shouldLogDebug()) logFixActionsForDebug(allFixActions);
//        List<FixAction> validFixes = validation(allFixActions, fixActionMaps);
//        secondValidation(validFixes, fixActionMaps);


    }
    public void DetectionWork() throws IOException {
        Detector detector = new Detector();
        detector.setMethodToMonitorForBug(javaProject);
        //修改当前project的bug程序路径，将它改成补丁程序的路径
        javaProject.setSourceDirs(javaProject.getPatchSourceDir());
        javaProject.setSourceFiles(javaProject.getPatchSourceFiles());
        javaProject.setOutputDir(javaProject.getPatchOutputDir());
        ClassToFixPreprocessor preprocessor = new ClassToFixPreprocessor(javaProject, config);
        preprocessor.preprocess();
        javaProject.registerMethodToMonitor(config);
        javaProject.initMethodToMonitor();
        javaProject.compile();
        if(javaProject.getStateSnapshotFive().getNewTestsValueMap() != null)
        detector.setNewTestsValueMap_Bug(javaProject.getStateSnapshotFive().getNewTestsValueMap());
        detector.setRealValueFalsingTestsMap_Bug(javaProject.getStateSnapshotFive().getRealValueFalsingTestsMap());
        detector.setRealValuePassingTestsMap_Bug(javaProject.getStateSnapshotFive().getRealValuePassingTestsMap());
        detector.setMethodToMonitorForPatch(javaProject);
        detector.collectAllTestRequest();
        detector.setStateSnapshot(javaProject.getStateSnapshotFive().getStateSnapshot());
        javaProject.setDetector(detector);

        ProgramMonitor_Patch programMonitor_patch = new ProgramMonitor_Patch(config);
        programMonitor_patch.execute_Patch();
        List<TestExecutionResult> allTestResults = programMonitor_patch.monitorProgramStates(javaProject, javaProject.getDetector().passing,
                javaProject.getDetector().allTestsToDebug);

        detector.distributeTestExecutionResult(allTestResults);


        detector.detectCorrect();
        recordAnswer(detector, javaProject);





    }
    public void recordAnswer(Detector detector, JavaProject javaProject) throws IOException{
        File file;
        if(detector.isOverfitting){
            file = new File(javaProject.getRootDir() + "\\overfitting.txt");
        }
        else {
            file = new File(javaProject.getRootDir() + "\\correct.txt");
        }
        FileWriter fileWriter = new FileWriter(file);
        PrintWriter printWriter = new PrintWriter(fileWriter);

        printWriter.println("lineLocationToMonitor : " + javaProject.getMethodToMonitor().getMethodDeclarationInfoCenter()
                .getRelevantLocationStatementMap().get(detector.lineLocationToMonitor));
        printWriter.println("SnapshotExpression : " + detector.stateSnapshot.getSnapshotExpression() + " " + detector.stateSnapshot.getValue());
        printWriter.println("NP : " + detector.NP + "   NF : " + detector.NF);
        printWriter.println("测试用例中表达式对比 ");
        printWriter.println("原测试集-------");
        printWriter.println("成功测试用例");
        for(Map.Entry<TestRequest, Boolean> entry : detector.getRealValuePassingTestsMap_Patch().entrySet()){
            Boolean bugBoo = detector.getRealValuePassingTestsMap_Bug().get(entry.getKey());
            if(bugBoo.booleanValue() != entry.getValue().booleanValue()){
                printWriter.println("TestSign : " + entry.getKey().getTestClassAndMethod() +
                        "    " + "bug程序: " + bugBoo.booleanValue() + "     " + "补丁程序: " + entry.getValue().booleanValue());
            }
        }

        printWriter.println("失败测试用例");
        for (Map.Entry<TestRequest, Boolean> entry : detector.getRealValueFalsingTestsMap_Patch().entrySet()){
            Boolean bugBoo = detector.getRealValueFalsingTestsMap_Bug().get(entry.getKey());
            if (bugBoo.booleanValue() == entry.getValue().booleanValue()){
                printWriter.println("TestSign : " + entry.getKey().getTestClassAndMethod() +
                        "    " + "bug程序: " + bugBoo.booleanValue() + "     " + "补丁程序: " + entry.getValue().booleanValue());
            }
        }

        printWriter.println("新的测试用例");
        for(Map.Entry<TestRequest, Boolean> entry : detector.getNewTestsValueMap_Patch().entrySet()){
            Boolean bugBoo = detector.getNewTestsValueMap_Bug().get(entry.getKey());
            if(bugBoo.booleanValue() == entry.getValue().booleanValue()){
                printWriter.println("TestSign : " + entry.getKey().getTestClassAndMethod() +
                        "    " + "bug程序: " + bugBoo.booleanValue() + "     " + "补丁程序: " + entry.getValue().booleanValue());
            }
        }
        printWriter.close();
    }
    public static void startNewTests(JavaProject javaProject,Map<AbsSuspiciousnessAlgorithm.SbflAlgorithm,
            List<StateSnapshot>> stateSnapshots,ProgramMonitor programMonitor) throws IOException {


        StateSnapshot stateSnapshotf = stateSnapshots.get(AbsSuspiciousnessAlgorithm.SbflAlgorithm.AutoFix).get(0);
        StateSnapshotFive stateSnapshotFive = creatStateSnapshotFive(stateSnapshotf);
        javaProject.setStateSnapshotFive(stateSnapshotFive);
        EvosuiteT evosuiteT = creatEvosuiteT(javaProject);
//        evosuiteT.createTests();
        stateSnapshotFive.setEvosuiteT(evosuiteT);
        getTestRealValue(stateSnapshotf, programMonitor.getTestResults(), javaProject);
        if(stateSnapshotFive.evosuiteTSources != null){
            javaProject.compileNewTest();

            evosuiteT.setNewTestRequest(javaProject.getAllNewTestsToRun());
            evosuiteT.setTestsCoveredBugMethod(new ArrayList<>());
            evosuiteT.setTestExecutionResultsCoveredBug(new ArrayList<>());

            debugNewTests(javaProject);

            //保存测试用例执行时的程序状态
            List<TestExecutionResult> testExecutionResults = programMonitor.monitorProgramStates(javaProject, evosuiteT.getTestExecutionResultsCoveredBug(), evosuiteT.getTestsCoveredBugMethod());
            getNewTestRealValue(stateSnapshotf, testExecutionResults, javaProject);
        }



    }
    //将测试用例过滤出来
    public static void debugNewTests(JavaProject javaProject){
        TestByLocationSelector testByLocationSelector = new TestByLocationSelector(javaProject,
                Session.getSession().getConfig().getLogLevel(),
                javaProject.getTimeoutPerTest() * 50, FailureHandling.CONTINUE);
        testByLocationSelector.launch();
        testByLocationSelector.pruneIrrelevantLocationsAndTests();
    }
    public static EvosuiteT creatEvosuiteT(JavaProject javaProject){
        EvosuiteT evosuiteT = new EvosuiteT();
        evosuiteT.setProject(javaProject);
        return evosuiteT;
    }
    public static StateSnapshotFive creatStateSnapshotFive(StateSnapshot stateSnapshotf){

        StateSnapshotFive stateSnapshotFive = new StateSnapshotFive();
        stateSnapshotFive.setStateSnapshot(stateSnapshotf);
        return stateSnapshotFive;
    }

    //找出表达式对应的测试结果的真实值
    public static void getTestRealValue(StateSnapshot stateSnapshot, List<TestExecutionResult> testResults, JavaProject javaProject){

        Map<TestRequest, Boolean> realValuePassingTestsMap = new HashMap<>();
        Map<TestRequest, Boolean> realValueFalsingTestsMap = new HashMap<>();

        Map<String, TestRequest> passingTestsMap = javaProject.getPassingTests().stream().collect(Collectors.toMap(TestRequest::getTestClassAndMethod, Function.identity()));
        Map<String, TestRequest> falsingTestsMap = javaProject.getFailingTests().stream().collect(Collectors.toMap(TestRequest::getTestClassAndMethod, Function.identity()));

        for(TestExecutionResult testExecutionResult : testResults){
                for (ProgramState programState : testExecutionResult.getObservedStates()){
                    //关键一个方法
                    DebuggerEvaluationResult evaluationResult = stateSnapshot.getSnapshotExpression().evaluate(programState);
                    if (evaluationResult.hasSemanticError() || evaluationResult.hasSyntaxError())
                        continue;
                    if (!(evaluationResult instanceof DebuggerEvaluationResult.BooleanDebuggerEvaluationResult))
                        throw new IllegalStateException();
                    boolean booleanEvaluationResult = ((DebuggerEvaluationResult.BooleanDebuggerEvaluationResult) evaluationResult).getValue();
                    if(programState.getLocation().getLineNo() == stateSnapshot.getLocation().getLineNo()){
//                        realValueMap.put(testExecutionResult, booleanEvaluationResult);

                        if(passingTestsMap.containsKey(testExecutionResult.getTestClassAndMethod())){
                            realValuePassingTestsMap.put(passingTestsMap.get(testExecutionResult.getTestClassAndMethod()), booleanEvaluationResult);
                        }
                        else if(falsingTestsMap.containsKey(testExecutionResult.getTestClassAndMethod())){
                            realValueFalsingTestsMap.put(falsingTestsMap.get(testExecutionResult.getTestClassAndMethod()), booleanEvaluationResult);
                        }
                    }
                }

        }
        System.out.println("realValueFalsingTestsMap.size : " + realValueFalsingTestsMap.size());
        System.out.println("realValuePassingTestsMap.size : " + realValuePassingTestsMap.size());
        javaProject.getStateSnapshotFive().setRealValueFalsingTestsMap(realValueFalsingTestsMap);
        javaProject.getStateSnapshotFive().setRealValuePassingTestsMap(realValuePassingTestsMap);
    }

    //匹配新测试用例中对应表达式的
    public static void getNewTestRealValue(StateSnapshot stateSnapshot, List<TestExecutionResult> testResults, JavaProject javaProject){
        boolean stateSnapshotValue =((DebuggerEvaluationResult.BooleanDebuggerEvaluationResult)stateSnapshot.getValue()).getValue();
        Map<TestRequest, Boolean> realValueMap = new HashMap<>();

        Map<String, TestRequest> testRequestsSet = javaProject.getAllNewTestsToRun().stream().collect(Collectors.toMap(TestRequest::getTestMethod, Function.identity()));

        for(TestExecutionResult testExecutionResult : testResults){
            for (ProgramState programState : testExecutionResult.getObservedStates()){
                //关键一个方法
                DebuggerEvaluationResult evaluationResult = stateSnapshot.getSnapshotExpression().evaluate(programState);
                if (evaluationResult.hasSemanticError() || evaluationResult.hasSyntaxError())
                    continue;
                if (!(evaluationResult instanceof DebuggerEvaluationResult.BooleanDebuggerEvaluationResult))
                    throw new IllegalStateException();
                boolean booleanEvaluationResult = ((DebuggerEvaluationResult.BooleanDebuggerEvaluationResult) evaluationResult).getValue();
                if(programState.getLocation().getLineNo() == stateSnapshot.getLocation().getLineNo() && booleanEvaluationResult == stateSnapshotValue){
                    realValueMap.put(testRequestsSet.get(testExecutionResult.getTestMethod()), booleanEvaluationResult);
                }
            }
        }
        javaProject.getStateSnapshotFive().setNewTestsValueMap(realValueMap);

    }
    private Map<LineLocation, Set<FixAction>> generateFixActions(List<StateSnapshot> snapshots) {
        // fixme: originally, enableBasicStrategies are not used in comprehensive mode. Is that really what we want?
        SnippetBuilder snippetBuilder = new SnippetBuilder();
        snippetBuilder.enableBasicStrategies();
        snippetBuilder.enableComprehensiveStrategies(Session.getSession().getConfig()
                .getSnippetConstructionStrategy() != Config.SnippetConstructionStrategy.BASIC);

        for (StateSnapshot snapshot : snapshots) {
            snippetBuilder.buildSnippets(snapshot);
        }

        Map<StateSnapshot, Set<Snippet>> snippets = snippetBuilder.getSnippets();

        LoggingService.infoAll("Finish building snippets");
        if (shouldLogDebug()) logSnippetsForDebug(snippets);

        FixActionBuilder fixActionBuilder = new FixActionBuilder(javaProject);
        for (Map.Entry<StateSnapshot, Set<Snippet>> snippetEntry : snippets.entrySet()) {
            StateSnapshot snapshot = snippetEntry.getKey();
            for (Snippet snippet : snippetEntry.getValue()) {
                fixActionBuilder.buildFixActions(snapshot, snippet);
            }
        }
        Map<LineLocation, Set<FixAction>> fixActions = fixActionBuilder.getFixActionMap();

        LoggingService.infoAll("Finish building fixes");
        return fixActions;
    }


    private void removeAllIllformedFixActions(List<FixAction> fixActions) {
        IllformedFixActionsRemover remover = new IllformedFixActionsRemover(javaProject);
        int nbrRemoved = 0, totalRemoved = 0;

        // repeatedly remove all ill-formed fix actions
        do {
            nbrRemoved = remover.removeIllformedFixActions(fixActions);
            totalRemoved += nbrRemoved;

            if (shouldLogDebug()) {
                LoggingService.debugFileOnly("Number of ill-formed fix actions removed: "
                        + nbrRemoved + " in this round, " + totalRemoved + " int total.", COMPILATION_ERRORS);
            }
        } while (nbrRemoved != 0);
    }

    private List<FixAction> validation(List<FixAction> allFixActions, Map<AbsSuspiciousnessAlgorithm.SbflAlgorithm, List<Long>> sortedFixActionMaps) {
        // Sort all fix actions for validation by suspicious score. Using AutoFix score as default
        allFixActions.sort((o1, o2) -> Double.compare(o2.getStateSnapshot().getSuspiciousness(), o1.getStateSnapshot().getSuspiciousness()));

        BatchFixValidator batchFixValidator = new BatchFixValidator(javaProject, allFixActions);
        List<FixAction> validFixes = batchFixValidator.validateFixActions();

        // Mapping valid fix action to corresponding fl algorithm
        Map<AbsSuspiciousnessAlgorithm.SbflAlgorithm, List<Long>> validFixesMap = new HashMap<>();
        for (AbsSuspiciousnessAlgorithm.SbflAlgorithm sbflAlgorithm : sortedFixActionMaps.keySet()) {
            List<Long> validFixIds = validFixes.stream().map(FixAction::getFixId).collect(Collectors.toList());
            List<Long> sbflValidFixIds = new ArrayList<>();

            for (Long fix_id:sortedFixActionMaps.get(sbflAlgorithm)){
                if (validFixIds.contains(fix_id)) sbflValidFixIds.add(fix_id);
            }
            validFixesMap.put(sbflAlgorithm, sbflValidFixIds);
        }
        if (shouldLogDebug()) {
            for (AbsSuspiciousnessAlgorithm.SbflAlgorithm sbflAlgorithm : validFixesMap.keySet()) {
                LoggingService.debugFileOnly(sbflAlgorithm + " valid fixes", PLAUSIBLE_LOG);
                int rank = 0;
                for (Long aLong : validFixesMap.get(sbflAlgorithm)) {
                    LoggingService.debugFileOnly(++rank + " :: " + aLong, PLAUSIBLE_LOG);

                }
            }
        }
        return validFixes;
    }

    /**
     * This is used by ICJ only (using all test cases (white-box tests) to check the correctness of valid fixes)
     *
     * @param validFixes
     * @param fixActionMaps
     */
    private void secondValidation(List<FixAction> validFixes, Map<AbsSuspiciousnessAlgorithm.SbflAlgorithm, List<Long>> fixActionMaps) {
        if (config.getExperimentControl().isEnableSecondValidation()) {
            BatchFixValidator secondValidator = new BatchFixValidator.SecondBFValidator4ICJ(javaProject, validFixes);
            List<FixAction> secondValidFixes = secondValidator.validateFixActions();

            // Mapping 'correct' fix action to corresponding fl algorithm
            Map<AbsSuspiciousnessAlgorithm.SbflAlgorithm, List<Long>> correctFixesMap = new HashMap<>();
            for (AbsSuspiciousnessAlgorithm.SbflAlgorithm sbflAlgorithm : fixActionMaps.keySet()) {
                List<Long> correctFixIds = new ArrayList<>(secondValidFixes.stream().map(FixAction::getFixId).collect(Collectors.toList()));
                Collections.sort(correctFixIds, Comparator.comparingInt(o -> fixActionMaps.get(sbflAlgorithm).indexOf(o)));
                correctFixesMap.put(sbflAlgorithm, correctFixIds);
            }
            if (shouldLogDebug()) {
                for (AbsSuspiciousnessAlgorithm.SbflAlgorithm sbflAlgorithm : correctFixesMap.keySet()) {
                    LoggingService.debugFileOnly(sbflAlgorithm + " valid fixes", SECOND_VALIDATION_LOG);
                    int rank = 0;
                    for (Long aLong : correctFixesMap.get(sbflAlgorithm)) {
                        LoggingService.debugFileOnly(++rank + " :: " + aLong, SECOND_VALIDATION_LOG);

                    }
                }
            }
        }
        removeExtraLogger(SECOND_VALIDATION_LOG);
    }


    public double computeSimilarity(double locationSimilarity, double stateSimilarity) {
        return 2.0 / (1.0 / locationSimilarity + 1.0 / stateSimilarity);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
