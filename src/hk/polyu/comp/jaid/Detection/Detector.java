package hk.polyu.comp.jaid.Detection;

import hk.polyu.comp.jaid.java.JavaProject;
import hk.polyu.comp.jaid.monitor.LineLocation;
import hk.polyu.comp.jaid.monitor.MethodToMonitor;
import hk.polyu.comp.jaid.monitor.snapshot.StateSnapshot;
import hk.polyu.comp.jaid.monitor.snapshot.StateSnapshotFive;
import hk.polyu.comp.jaid.monitor.state.DebuggerEvaluationResult;
import hk.polyu.comp.jaid.monitor.state.ProgramState;
import hk.polyu.comp.jaid.test.TestExecutionResult;
import hk.polyu.comp.jaid.tester.TestRequest;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.Statement;
import org.evosuite.runtime.mock.java.net.SocketOut;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Detector {
    //bug程序中运行的数据
    public Map<TestRequest, Boolean> realValuePassingTestsMap_Bug;
    public Map<TestRequest, Boolean> realValueFalsingTestsMap_Bug;
    public Map<TestRequest, Boolean> newTestsValueMap_Bug;
    public MethodToMonitor methodToMonitor_Bug;

    //补丁程序运行的数据
    public MethodToMonitor methodToMonitor_Patch;
    public Map<TestRequest, Boolean> realValuePassingTestsMap_Patch;
    public Map<TestRequest, Boolean> realValueFalsingTestsMap_Patch;
    public Map<TestRequest, Boolean> newTestsValueMap_Patch;
    public List<TestExecutionResult> passing;
    public List<TestExecutionResult> falsing;
    public List<TestExecutionResult> newTest;
    public int NP = 0;//对成功测试用例
    public int NF = 0;//对失败测试用例

    public List<TestRequest> allTestsToDebug;
    public LineLocation lineLocationToMonitor;
    public StateSnapshot stateSnapshot;
    public List<TestExecutionResult> allTestResults;

    public boolean isOverfitting;

//    private static String returnStatement = "ReturnStatement";
//    private static String expressionStatement = "ExpressionStatement";
//    private static String variableDeclarationStatement = "VariableDeclarationStatement";
//    private static String ifStatement = "IfStatement";
//    private static String whileStatement = "WhileStatement";
//    private static String tryStatement = "TryStatement";
//    private static String forStatement = "ForStatement";

    public void detectCorrect(){
        determineLineLocation();
        if(lineLocationToMonitor != null && methodToMonitor_Patch.getMethodDeclarationInfoCenter()
                .getRelevantLocationStatementMap().get(lineLocationToMonitor) != null){
            getAllTestRealValue_Patch();
            comparePatchAndBugResult();
            System.out.println();
        }
        else{
            cannotFindLineLocationToMonitor();
        }
        System.out.println(stateSnapshot.getLocation() + " " + stateSnapshot.getSnapshotExpression());
        System.out.println(methodToMonitor_Patch.getMethodDeclarationInfoCenter().getRelevantLocationStatementMap().get(lineLocationToMonitor));
        System.out.println("NP : " + NP + " NF: " + NF);
        System.out.println(lineLocationToMonitor);
        System.out.println(realValuePassingTestsMap_Patch.size() + " " + realValueFalsingTestsMap_Patch.size());
        System.out.println(realValuePassingTestsMap_Bug.size() + " " + realValueFalsingTestsMap_Bug.size());
    }

    private void cannotFindLineLocationToMonitor(){
        System.out.println("没有找到监听的语句");
    }

    private void comparePatchAndBugResult(){


        //成功的测试用例
        for(Map.Entry<TestRequest, Boolean> entry : realValuePassingTestsMap_Patch.entrySet()){
            Boolean bugBoo = realValuePassingTestsMap_Bug.get(entry.getKey());
            if(bugBoo.booleanValue() != entry.getValue().booleanValue()){
                NP++;
            }
        }
        //失败的测试用例
        for(Map.Entry<TestRequest, Boolean> entry : realValueFalsingTestsMap_Patch.entrySet()){
            Boolean bugBoo = realValueFalsingTestsMap_Bug.get(entry.getKey());
            if(bugBoo.booleanValue() == entry.getValue().booleanValue()){
                NF++;
            }
        }

        //新生成的测试用例
        for(Map.Entry<TestRequest, Boolean> entry : newTestsValueMap_Patch.entrySet()){
            Boolean bugBoo = newTestsValueMap_Bug.get(entry.getKey());

            if(bugBoo.booleanValue() == entry.getValue().booleanValue()){
                NF++;
            }
        }
        if(NF == 0 && NP == 0) {
            System.out.println("正确的");
            isOverfitting = false;
        }
        else{
            System.out.println("失败的");
            isOverfitting = true;
        }
    }




    //获取补丁程序运行测试用例后的表达式的值
    private void getAllTestRealValue_Patch(){
        List<TestExecutionResult> testResults = getAllTestResults();
        Map<LineLocation, Statement> relevantLocationStatementMap = methodToMonitor_Patch.getMethodDeclarationInfoCenter()
                .getRelevantLocationStatementMap();
        realValuePassingTestsMap_Patch = new HashMap<>();
        realValueFalsingTestsMap_Patch = new HashMap<>();
        newTestsValueMap_Patch = new HashMap<>();
        Map<String, TestRequest> passingTestRequest = realValuePassingTestsMap_Bug.keySet().stream().collect(
                Collectors.toMap(TestRequest::getTestClassAndMethod, Function.identity())
        );
        Map<String, TestRequest> falsingTestRequest = realValueFalsingTestsMap_Bug.keySet().stream().collect(
                Collectors.toMap(TestRequest::getTestClassAndMethod, Function.identity())
        );
        Map<String, TestRequest> newTests = new HashMap<>();
        if(newTestsValueMap_Bug != null){
            newTests = newTestsValueMap_Bug.keySet().stream().collect(
                    Collectors.toMap(TestRequest::getTestClassAndMethod, Function.identity())
            );
        }

//        System.out.println(relevantLocationStatementMap);

        for(TestExecutionResult testExecutionResult : testResults){
            System.out.println("test : " + testExecutionResult.getTestClassAndMethod());
            for (ProgramState programState : testExecutionResult.getObservedStates()){
//                System.out.println(programState.getLocation());
                //关键一个方法
                DebuggerEvaluationResult evaluationResult = stateSnapshot.getSnapshotExpression().evaluate(programState);
                if (evaluationResult.hasSemanticError() || evaluationResult.hasSyntaxError())
                    continue;
                if (!(evaluationResult instanceof DebuggerEvaluationResult.BooleanDebuggerEvaluationResult))
                    throw new IllegalStateException();
                boolean booleanEvaluationResult = ((DebuggerEvaluationResult.BooleanDebuggerEvaluationResult) evaluationResult)
                        .getValue();
//                System.out.println(lineLocationToMonitor.toString());
//                System.out.println(programState.getLocation().toString());
//                if(relevantLocationStatementMap.get(lineLocationToMonitor).toString().equals(
//                        relevantLocationStatementMap.get(programState.getLocation()).toString()
//                )){
//                    System.out.println(lineLocationToMonitor.toString());
//                    System.out.println(programState.getLocation().toString());
                    setTestRequestMapsValue(testExecutionResult, booleanEvaluationResult, passingTestRequest,
                            falsingTestRequest, newTests);
//                }
            }
        }
    }
    private void setTestRequestMapsValue(TestExecutionResult result, boolean boo, Map<String, TestRequest> passingTestRequest,
                                         Map<String, TestRequest> falsingTestRequest, Map<String, TestRequest> newTests){
        String testClassAndMethod = result.getTestClassAndMethod();
//        System.out.println("testName : " + testClassAndMethod);
        if(passingTestRequest.containsKey(testClassAndMethod)){
            realValuePassingTestsMap_Patch.put(passingTestRequest.get(testClassAndMethod), boo);
        }
        else if(falsingTestRequest.containsKey(testClassAndMethod)){
            realValueFalsingTestsMap_Patch.put(falsingTestRequest.get(testClassAndMethod), boo);
        }
        else {
            newTestsValueMap_Patch.put(newTests.get(testClassAndMethod), boo);
        }
    }
    private void determineLineLocation(){

        List<LineLocation> allLocations_bug = new ArrayList<>(methodToMonitor_Bug.getMethodDeclarationInfoCenter().getAllLocationStatementMap().keySet());
        List<LineLocation> allLocations_patch = new ArrayList<>(methodToMonitor_Patch.getMethodDeclarationInfoCenter().getAllLocationStatementMap().keySet());
        allLocations_bug.sort(Comparator.comparing(LineLocation :: getLineNo));
        allLocations_patch.sort(Comparator.comparing(LineLocation :: getLineNo));

        obtainStartandEndDiffStatements(allLocations_bug, allLocations_patch);
    }

    //获取两个程序不同处的开始与结束语句
    private void obtainStartandEndDiffStatements(List<LineLocation> allLocations_bug, List<LineLocation> allLocations_patch){
        boolean isExist = false;
        int start = 0, end = 0;
        Map<LineLocation,Statement> patchMap = methodToMonitor_Patch.getMethodDeclarationInfoCenter().getAllLocationStatementMap();
        Map<LineLocation,Statement> bugMap = methodToMonitor_Bug.getMethodDeclarationInfoCenter().getAllLocationStatementMap();
        //从程序开始处查找
        int index = 0;
        for(int patch = 0, bug = 0; patch < allLocations_patch.size() && bug < allLocations_bug.size();){
            Statement statement_P = patchMap.get(allLocations_patch.get(patch));
            Statement statement_B = bugMap.get(allLocations_bug.get(bug));
            if(!statement_B.toString().equals(statement_P.toString())){
                isExist = true;
                start = patch;
                break;
            }
            patch++;
            bug++;
            index = patch;
        }
        if(!isExist){
            if(index <= allLocations_patch.size() - 1){
                start = index;
            }
            else start = index - 1;
        }

        isExist = false;
        //从程序结尾向上查找
        index = allLocations_patch.size() - 1;
        for(int patch = allLocations_patch.size() - 1, bug = allLocations_bug.size() - 1; patch >= 0 && bug >= 0;){
            Statement statement_P = patchMap.get(allLocations_patch.get(patch));
            Statement statement_B = bugMap.get(allLocations_bug.get(bug));
            if(!statement_B.toString().equals(statement_P.toString())){
                isExist = true;
                end = patch;
                break;
            }
            patch--;
            bug--;
            index = patch;
        }
        if(!isExist){
            if(index >= 0){
                end = index;
            }
            else end = index;
        }
        //至此确定start和end语句
        obtainLineLocationToMonitor(start, end, allLocations_patch);
    }
    private void obtainLineLocationToMonitor(int start, int end, List<LineLocation> allLocations_patch){
        if(end < start){
            lineLocationToMonitor = allLocations_patch.get(start);
            return;
        }
        Map<LineLocation,Statement> patchMap = methodToMonitor_Patch.getMethodDeclarationInfoCenter().getAllLocationStatementMap();
        Statement statement_Start = patchMap.get(allLocations_patch.get(start));
        Statement statement_End = patchMap.get(allLocations_patch.get(end));
        //如果start包含语句end，即start是if块、for、while等语句，end是这些块中的语句，则读取这个块的下一句。
        if(isMatch(statement_Start, statement_End)){
            //存在下一句
            if(end != allLocations_patch.size() - 1){
                lineLocationToMonitor = allLocations_patch.get(end + 1);
            }
            else{
                lineLocationToMonitor = allLocations_patch.get(end);
            }
        }
        else {
            //如果start和end不是包含关系，那么读取end的下一句
            //如果不存在下一条语句，则读取end语句
            if(end == allLocations_patch.size() - 1){
                lineLocationToMonitor = allLocations_patch.get(end);
            }
            else{
                //如果end语句的下一条语句和end在同一个块里，那么就去end的下一条语句
                Statement statement_NextToEnd = patchMap.get(allLocations_patch.get(end + 1));
                if(statement_NextToEnd.getParent().equals(statement_End) || statement_NextToEnd.getParent().equals(statement_End.getParent())){
                    lineLocationToMonitor = allLocations_patch.get(end + 1);
                }
                else lineLocationToMonitor = allLocations_patch.get(end);
            }
        }
    }
    //判断语句start是否包含语句end
    private boolean isMatch(Statement start, Statement end){
        boolean isMatched = false;
        Statement p = (Statement) end.getParent();
        while(!p.getClass().getSimpleName().equals("MethodDeclaration")){
            if(p.equals(start)){
                isMatched = true;
                break;
            }
            if(!p.getParent().getClass().getSimpleName().equals("MethodDeclaration")){
                p = (Statement) p.getParent();
            }
            else {
                break;
            }

        }
        return isMatched;
    }
//    public void isDifferent(List<LineLocation> relaventLocations_bug, List<LineLocation> relaventLocations_patch){
//        int bugIndex = relaventLocations_bug.size() - 1;
//        int patchIndex = relaventLocations_patch.size() - 1;
//        boolean isDiff = false;
//        int flag = 0;
//
//        while(bugIndex >= 0 && patchIndex >= 0){
//            //当出现不同的语句时
//            Statement patchStatement = methodToMonitor_Patch.getMethodDeclarationInfoCenter().
//                    getRelevantLocationStatementMap().get(relaventLocations_patch.get(patchIndex));
//            Statement bugStatement = methodToMonitor_Bug.getMethodDeclarationInfoCenter().
//                    getRelevantLocationStatementMap().get(relaventLocations_bug.get(bugIndex));
//            if(!patchStatement.toString().equals(bugStatement.toString())){
//                flag = bugIndex;
//                isDiff = isHaveLineLocation(bugIndex, relaventLocations_patch);
//                break;
//            }
//            bugIndex--;
//            patchIndex--;
//        }
//        if(isDiff == false){
//            lineLocationToMonitor = relaventLocations_patch.get(flag);
//        }
//
//    }
//    public boolean isHaveLineLocation(int bugIndex, List<LineLocation> relaventLocations_patch){
////        String simpleName = relaventLocations_patch.get(bugIndex).getStatement().getClass().getSimpleName();
//        Map<LineLocation,Statement> patchMap = methodToMonitor_Patch.getMethodDeclarationInfoCenter().getRelevantLocationStatementMap();
//        String simpleName = patchMap.get(relaventLocations_patch.get(bugIndex)).getClass().getSimpleName();
//        if(simpleName.equals(expressionStatement)
//        || simpleName.equals(variableDeclarationStatement) || simpleName.equals(returnStatement)){
//            if(bugIndex < relaventLocations_patch.size() - 1){
//                lineLocationToMonitor = relaventLocations_patch.get(bugIndex + 1);
//                return true;
//            }
//            else return false;
//        }
//        if(simpleName.equals(whileStatement) || simpleName.equals(ifStatement) ||
//        simpleName.equals(tryStatement) || simpleName.equals(forStatement)){
////            LineLocation parent = relaventLocations_patch.get(bugIndex);
//            Statement parent = patchMap.get(relaventLocations_patch.get(bugIndex));
//            bugIndex++;
//            while (bugIndex < relaventLocations_patch.size()){
//                if(!patchMap.get(relaventLocations_patch.get(bugIndex)).getParent().equals(parent)){
//                    lineLocationToMonitor = relaventLocations_patch.get(bugIndex);
//                    return true;
//                }
//                bugIndex++;
//            }
//        }
//        return false;
//    }
    //将所有的测试用例的结果放到对应的里面
    public void distributeTestExecutionResult(List<TestExecutionResult> allTestResults){
        passing = new ArrayList<>();
        falsing = new ArrayList<>();
        newTest = new ArrayList<>();
        setAllTestResults(allTestResults);
        Map<String, TestRequest> realValuePassingTests = realValuePassingTestsMap_Bug.keySet().stream().collect(
                Collectors.toMap(TestRequest::getTestClassAndMethod, Function.identity())
        );
        Map<String, TestRequest> realValueFalsingTests = realValueFalsingTestsMap_Bug.keySet().stream().collect(
                Collectors.toMap(TestRequest::getTestClassAndMethod, Function.identity())
        );
        for(TestExecutionResult testExecutionResult : allTestResults){
            if(realValuePassingTests.containsKey(testExecutionResult.getTestClassAndMethod())){
                passing.add(testExecutionResult);
            }
            else if(realValueFalsingTests.containsKey(testExecutionResult.getTestClassAndMethod())){
                falsing.add(testExecutionResult);
            }
            else{
                newTest.add(testExecutionResult);
            }
        }
    }
    public void setMethodToMonitorForBug(JavaProject javaProject){
        this.setMethodToMonitor_Bug(javaProject.getMethodToMonitor());
    }
    public void setMethodToMonitorForPatch(JavaProject javaProject){
        this.setMethodToMonitor_Patch(javaProject.getMethodToMonitor());
    }
//一次性将测试用例请求全部整合起来
    public void collectAllTestRequest(){
        if(allTestsToDebug == null) allTestsToDebug = new ArrayList<>();
        if(getNewTestsValueMap_Bug() != null)
        allTestsToDebug.addAll(getNewTestsValueMap_Bug().keySet());
        allTestsToDebug.addAll(getRealValuePassingTestsMap_Bug().keySet());
        allTestsToDebug.addAll(getRealValueFalsingTestsMap_Bug().keySet());
    }
    //Getter && Setter


    public List<TestExecutionResult> getAllTestResults() {
        return allTestResults;
    }

    public void setAllTestResults(List<TestExecutionResult> allTestResults) {
        this.allTestResults = allTestResults;
    }

    public StateSnapshot getStateSnapshot() {
        return stateSnapshot;
    }

    public void setStateSnapshot(StateSnapshot stateSnapshot) {
        this.stateSnapshot = stateSnapshot;
    }

    public List<TestRequest> getAllTestsToDebug() {
        return allTestsToDebug;
    }

    public void setAllTestsToDebug(List<TestRequest> allTestsToDebug) {
        this.allTestsToDebug = allTestsToDebug;
    }

    public List<TestExecutionResult> getNewTest() {
        return newTest;
    }

    public void setNewTest(List<TestExecutionResult> newTest) {
        this.newTest = newTest;
    }

    public List<TestExecutionResult> getFalsing() {
        return falsing;
    }

    public void setFalsing(List<TestExecutionResult> falsing) {
        this.falsing = falsing;
    }

    public List<TestExecutionResult> getPassing() {
        return passing;
    }

    public void setPassing(List<TestExecutionResult> passing) {
        this.passing = passing;
    }

    public MethodToMonitor getMethodToMonitor_Patch() {
        return methodToMonitor_Patch;
    }

    public void setMethodToMonitor_Patch(MethodToMonitor methodToMonitor_Patch) {
        this.methodToMonitor_Patch = methodToMonitor_Patch;
    }

    public MethodToMonitor getMethodToMonitor_Bug() {
        return methodToMonitor_Bug;
    }

    public Map<TestRequest, Boolean> getNewTestsValueMap_Patch() {
        return newTestsValueMap_Patch;
    }

    public void setNewTestsValueMap_Patch(Map<TestRequest, Boolean> newTestsValueMap_Patch) {
        this.newTestsValueMap_Patch = newTestsValueMap_Patch;
    }

    public Map<TestRequest, Boolean> getRealValueFalsingTestsMap_Patch() {
        return realValueFalsingTestsMap_Patch;
    }

    public void setRealValueFalsingTestsMap_Patch(Map<TestRequest, Boolean> realValueFalsingTestsMap_Patch) {
        this.realValueFalsingTestsMap_Patch = realValueFalsingTestsMap_Patch;
    }

    public Map<TestRequest, Boolean> getRealValuePassingTestsMap_Patch() {
        return realValuePassingTestsMap_Patch;
    }

    public void setRealValuePassingTestsMap_Patch(Map<TestRequest, Boolean> realValuePassingTestsMap_Patch) {
        this.realValuePassingTestsMap_Patch = realValuePassingTestsMap_Patch;
    }

    public Map<TestRequest, Boolean> getNewTestsValueMap_Bug() {
        return newTestsValueMap_Bug;
    }

    public void setNewTestsValueMap_Bug(Map<TestRequest, Boolean> newTestsValueMap_Bug) {
        this.newTestsValueMap_Bug = newTestsValueMap_Bug;
    }

    public Map<TestRequest, Boolean> getRealValueFalsingTestsMap_Bug() {
        return realValueFalsingTestsMap_Bug;
    }

    public void setRealValueFalsingTestsMap_Bug(Map<TestRequest, Boolean> realValueFalsingTestsMap_Bug) {
        this.realValueFalsingTestsMap_Bug = realValueFalsingTestsMap_Bug;
    }

    public void setMethodToMonitor_Bug(MethodToMonitor methodToMonitor_Bug) {
        this.methodToMonitor_Bug = methodToMonitor_Bug;
    }

    public Map<TestRequest, Boolean> getRealValuePassingTestsMap_Bug() {
        return realValuePassingTestsMap_Bug;
    }

    public void setRealValuePassingTestsMap_Bug(Map<TestRequest, Boolean> realValuePassingTestsMap_Bug) {
        this.realValuePassingTestsMap_Bug = realValuePassingTestsMap_Bug;
    }

    public LineLocation getLineLocationToMonitor() {
        return lineLocationToMonitor;
    }

    public void setLineLocationToMonitor(LineLocation lineLocationToMonitor) {
        this.lineLocationToMonitor = lineLocationToMonitor;
    }
}
