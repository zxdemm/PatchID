package hk.polyu.comp.jaid.monitor.snapshot;

import hk.polyu.comp.jaid.test.TestExecutionResult;
import hk.polyu.comp.jaid.testCreator.EvosuiteT;
import hk.polyu.comp.jaid.tester.TestRequest;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class StateSnapshotFive {
           public StateSnapshot stateSnapshot;
           public Map<TestRequest, Boolean> realValuePassingTestsMap;
           public Map<TestRequest, Boolean> realValueFalsingTestsMap;
           public List<Path> evosuiteTSources;//新测试用例源码
           public Path evosiuteTFilePath;

           public String evosuiteJar = "input your path/jaid/evosuite.jar";
           public String jarPath = "input your path/jaid/evosuite-standalone-runtime.jar";
           public Map<TestRequest, Boolean> newTestsValueMap;
           public EvosuiteT evosuiteT;
    public List<Path> getEvosuiteTSource() {
        return evosuiteTSources;
    }

    public void setEvosuiteTSources(List<Path> evosuiteTSources){
        this.evosuiteTSources = evosuiteTSources;
    }

    public Map<TestRequest, Boolean> getNewTestsValueMap() {
        return newTestsValueMap;
    }

    public void setNewTestsValueMap(Map<TestRequest, Boolean> newTestsValueMap) {
        this.newTestsValueMap = newTestsValueMap;
    }

    public Path getEvosuiteTOutPut() {
        return EvosuiteTOutPut;
    }

    public void setEvosuiteTOutPut(Path evosuiteTOutPut) {
        EvosuiteTOutPut = evosuiteTOutPut;
    }

    public Path EvosuiteTOutPut;//新测试用例class

    public EvosuiteT getEvosuiteT() {
        return evosuiteT;
    }

    public void setEvosuiteT(EvosuiteT evosuiteT) {
        this.evosuiteT = evosuiteT;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getEvosuiteJar() {
        return evosuiteJar;
    }

    public void setEvosuiteJar(String evosuiteJar) {
        this.evosuiteJar = evosuiteJar;
    }

    public List<Path> getEvosuiteTSources() {
        return evosuiteTSources;
    }

    public StateSnapshotFive(){

    }
    public void setStateSnapshot(StateSnapshot stateSnapshot) {
        this.stateSnapshot = stateSnapshot;
    }



    public StateSnapshot getStateSnapshot() {
        return stateSnapshot;
    }

    public Map<TestRequest, Boolean> getRealValuePassingTestsMap() {
        return realValuePassingTestsMap;
    }

    public void setRealValuePassingTestsMap(Map<TestRequest, Boolean> realValuePassingTestsMap) {
        this.realValuePassingTestsMap = realValuePassingTestsMap;
    }

    public Map<TestRequest, Boolean> getRealValueFalsingTestsMap() {
        return realValueFalsingTestsMap;
    }

    public void setRealValueFalsingTestsMap(Map<TestRequest, Boolean> realValueFalsingTestsMap) {
        this.realValueFalsingTestsMap = realValueFalsingTestsMap;
    }

    public Path getEvosiuteTFilePath() {
        return evosiuteTFilePath;
    }

    public void setEvosiuteTFilePath(Path evosiuteTFilePath) {
        this.evosiuteTFilePath = evosiuteTFilePath;
    }
}
