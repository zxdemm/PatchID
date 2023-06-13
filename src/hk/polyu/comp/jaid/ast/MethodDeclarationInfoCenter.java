package hk.polyu.comp.jaid.ast;

import com.sun.jdi.Location;
import hk.polyu.comp.jaid.Detection.ClassToFixPreprocessor_Patch;
import hk.polyu.comp.jaid.fixer.config.FixerOutput;
import hk.polyu.comp.jaid.fixer.log.LoggingService;
import hk.polyu.comp.jaid.java.ClassToFixPreprocessor;
import hk.polyu.comp.jaid.monitor.ConstructedETM;
import hk.polyu.comp.jaid.monitor.ExpressionToMonitor;
import hk.polyu.comp.jaid.monitor.LineLocation;
import hk.polyu.comp.jaid.monitor.MethodToMonitor;
import hk.polyu.comp.jaid.monitor.snapshot.StateSnapshot;
import hk.polyu.comp.jaid.monitor.snapshot.StateSnapshotExpression;
import hk.polyu.comp.jaid.monitor.snapshot.StateSnapshotExpressionBuilder;
import hk.polyu.comp.jaid.monitor.state.DebuggerEvaluationResult;
import hk.polyu.comp.jaid.monitor.state.ProgramState;
import hk.polyu.comp.jaid.test.TestExecutionResult;
import hk.polyu.comp.jaid.tester.Tester;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static hk.polyu.comp.jaid.ast.ExpressionEnriching.*;
import static hk.polyu.comp.jaid.fixer.config.FixerOutput.LogFile.MONITORED_EXPS;
import static hk.polyu.comp.jaid.fixer.log.LoggingService.shouldLogDebug;
import static hk.polyu.comp.jaid.util.CommonUtils.newAstExpression;

/**
 * Created by Max PEI.
 * 这个类是
 */
public class MethodDeclarationInfoCenter {

    private final MethodToMonitor contextMethod;

    public MethodDeclarationInfoCenter(MethodToMonitor contextMethod) {
        this.contextMethod = contextMethod;
    }

    public void init_Patch(){
        originalBodyBlock = ((TryStatement) ((TryStatement) getMethodDeclaration().getBody().statements()
                .get(ClassToFixPreprocessor_Patch.getTryStatementIndex())).getBody().statements().get(0)).getBody();
        recordEntryAndExitLocation_Patch();
        constructLocationStatementMap();
        constructExpressionsAppearAtLocationMap();
        constructVarDefAssignMap();
        collectExpressionsToMonitor();
        registerExpressionsToLocation();
    }
    public void init() {
        originalBodyBlock = ((TryStatement) ((TryStatement) getMethodDeclaration().getBody().statements()
                .get(ClassToFixPreprocessor.getTryStatementIndex())).getBody().statements().get(0)).getBody();

        recordEntryAndExitLocation();
        constructLocationStatementMap();
        constructExpressionsAppearAtLocationMap();// maybe 'expressionsToMonitorNearLocation' is duplicated with this
        constructVarDefAssignMap();
        collectExpressionsToMonitor();
        registerExpressionsToLocation();
    }


    // ============================ Getters


    public Set<Statement> getIrRelevantStatements() {
        return irRelevantStatements;
    }

    public void setIrRelevantStatements(Set<Statement> irRelevantStatements) {
        this.irRelevantStatements = irRelevantStatements;
    }

    public MethodToMonitor getContextMethod() {
        return contextMethod;
    }

    public MethodDeclaration getMethodDeclaration() {
        return contextMethod.getMethodAST();
    }

    public LineLocation getExitLocation() {
        return exitLocation;
    }

    public void setAllLocationStatementMap(Map<LineLocation, Statement> allLocationStatementMap) {
        this.allLocationStatementMap = allLocationStatementMap;
    }

    public void setRelevantLocationStatementMap(Map<LineLocation, Statement> relevantLocationStatementMap) {
        this.relevantLocationStatementMap = relevantLocationStatementMap;
    }

    public LineLocation getEntryLocation() {
        return entryLocation;
    }


    public Statement getStatementAtLocation(LineLocation location) {
        return getAllLocationStatementMap().getOrDefault(location, null);
    }


    public ExpressionToMonitor getThisExpressionToMonitor() {
        if (thisExpressionToMonitor == null) {
            ThisExpression thisExp = getMethodDeclaration().getAST().newThisExpression();
            thisExpressionToMonitor = ExpressionToMonitor.construct(thisExp, getMethodDeclaration().resolveBinding().getDeclaringClass());
        }
        return thisExpressionToMonitor;
    }

    public ExpressionToMonitor getResultExpressionToMonitor() {
        if (resultExpressionToMonitor == null) {
            if (getContextMethod().returnsVoid())
                throw new IllegalStateException();

            VariableDeclarationStatement resultDeclaration = (VariableDeclarationStatement) getMethodDeclaration().getBody().statements().get(0);
            SimpleName resultExpression = ((VariableDeclarationFragment) resultDeclaration.fragments().get(0)).getName();
            if (!resultExpression.getIdentifier().equals(getContextMethod().getReturnVariableName()))
                throw new IllegalStateException();

            resultExpressionToMonitor = ExpressionToMonitor.construct(resultExpression, resultExpression.resolveTypeBinding());
        }
        return resultExpressionToMonitor;
    }

    public boolean isStaticMtf() {
        return Modifier.isStatic(getMethodDeclaration().getModifiers());
    }

    public Map<LineLocation, Statement> getRelevantLocationStatementMap() {
        return relevantLocationStatementMap;
    }

    public Map<LineLocation, Statement> getAllLocationStatementMap() {
        return allLocationStatementMap;
    }

    public SortedSet<ExpressionToMonitor> getExpressionsToMonitorWithSideEffect() {
        if (ExpressionsToMonitorWithSideEffect == null)
            ExpressionsToMonitorWithSideEffect = new TreeSet<>(ExpressionToMonitor.getByLengthComparator());
        return ExpressionsToMonitorWithSideEffect;
    }

    public Set<ExpressionToMonitor> getSideEffectFreeExpressionsToMonitor() {
        return getAllEnrichedEtmWithinMethod().stream().filter(ExpressionToMonitor::isSideEffectFree).collect(Collectors.toSet());
    }

    public SortedSet<ExpressionToMonitor> getBasicExpressions() {
        if (basicExpressions == null) {
            basicExpressions = new TreeSet<>(ExpressionToMonitor.getByLengthComparator());
        }
        return basicExpressions;
    }

    /**
     * Adjust this method to change the expressions used in the monitor stage
     *调整此方法以更改监视阶段中使用的表达式
     * @return
     */
    public SortedSet<ExpressionToMonitor> getAllEnrichedEtmWithinMethod() {
        TreeSet enrichedETM = new TreeSet<>(ExpressionToMonitor.getByLengthComparator());
        enrichedETM.addAll(getBasicExpressions());
        enrichedETM.addAll(getAllEnrichingEtmWithinMethod());
        return enrichedETM;
    }


    public SortedSet<ExpressionToMonitor> getAllEnrichingEtmWithinMethod() {
        if (allEnrichingExpressionsToMonitorWithinMethod == null) {
            allEnrichingExpressionsToMonitorWithinMethod = new TreeSet<>(ExpressionToMonitor.getByLengthComparator());
        }
        return allEnrichingExpressionsToMonitorWithinMethod;
    }

    public SortedSet<ExpressionToMonitor> getBooleanEnrichingEtmWithinMethod() {
        if (booleanEnrichingExpressionsToMonitorWithinMethod == null) {
            booleanEnrichingExpressionsToMonitorWithinMethod = new TreeSet<>(ExpressionToMonitor.getByLengthComparator());
        }
        return booleanEnrichingExpressionsToMonitorWithinMethod;
    }

    public SortedSet<ExpressionToMonitor> getReferenceEtmFields() {
        if (referenceFields == null) referenceFields = new TreeSet<>(ExpressionToMonitor.getByLengthComparator());
        return referenceFields;
    }

    public StateSnapshot getStateSnapshot(LineLocation location, StateSnapshotExpression expression, boolean value) {
        if (!getCategorizedStateSnapshotsWithinMethod().containsKey(location))
            throw new IllegalStateException();
        Map<StateSnapshotExpression, Map<Boolean, StateSnapshot>> subMap = getCategorizedStateSnapshotsWithinMethod().get(location);
        if (!subMap.containsKey(expression))
            throw new IllegalStateException();
        Map<Boolean, StateSnapshot> subsubMap = subMap.get(expression);
        return subsubMap.get(value);
    }

    public Set<StateSnapshotExpression> getStateSnapshotExpressionsWithinMethod() {
        return stateSnapshotExpressionsWithinMethod;
    }

    public Set<StateSnapshot> getStateSnapshotsWithinMethod() {
        return stateSnapshotsWithinMethod;
    }

    public Map<LineLocation, Double> getLocationDistanceToFailureMap() {
        if (locationDistanceToFailureMap == null)
            locationDistanceToFailureMap = new HashMap<>();

        return locationDistanceToFailureMap;
    }

    public Map<LineLocation, Set<ExpressionToMonitor>> getExpressionsAppearAtLocationMap() {
        return expressionsAppearAtLocationMap;
    }


    public Map<LineLocation, Map<StateSnapshotExpression, Map<Boolean, StateSnapshot>>> getCategorizedStateSnapshotsWithinMethod() {
        return categorizedStateSnapshotsWithinMethod;
    }

    public Set<ExpressionToMonitor> getConstantIntegers() {
        return constantIntegers;
    }

    public Map<LineLocation, SortedSet<ExpressionToMonitor>> getLocationExpressionMap() {
        return locationExpressionMap;
    }

    public Map<IVariableBinding, LineScope> getVariableDefinitionLocationMap() {
        return variableDefinitionLocationMap;
    }

    public Map<IVariableBinding, LineScope> getVariableAssignmentLocationMap() {
        return variableAssignmentLocationMap;
    }


    public Set<ExpressionToMonitor> getNearByLocationExpressionToMonitor(LineLocation lineLocation) {
        if (expressionsToMonitorNearLocation == null) expressionsToMonitorNearLocation = new HashMap<>();
        else if (expressionsToMonitorNearLocation.containsKey(lineLocation))
            return expressionsToMonitorNearLocation.get(lineLocation);

        Set<String> relatedExp = new HashSet<>();
        Set<ExpressionToMonitor> selectedExp = new HashSet<>();
        for (LineLocation l : getAllLocationStatementMap().keySet()) {
            int diff = Math.abs(lineLocation.getLineNo() - l.getLineNo());
            if (diff > 0 && diff < 3)
                for (ExpressionToMonitor appear : getExpressionsAppearAtLocationMap().get(lineLocation)) {
                    relatedExp.add(appear.getText());
                    for (ExpressionToMonitor subAppear : appear.getSubExpressions())
                        relatedExp.add(subAppear.getText());
                }
        }

        SortedSet<ExpressionToMonitor> expressions = getLocationExpressionMap().get(lineLocation);
        for (ExpressionToMonitor e : expressions) {
            if (e instanceof ConstructedETM && e.isMethodInvocation()) {
                MethodInvocation mi = (MethodInvocation) e.getExpressionAST();
                if (!relatedExp.contains(mi.getExpression().toString())) {
                    LoggingService.debug(lineLocation.getLineNo() + " :: not near constructed " + e.toString());
                    continue;
                }
            }
            selectedExp.add(e);
        }
        expressionsToMonitorNearLocation.put(lineLocation, selectedExp);
        return selectedExp;
    }

    // ============================ Implementation

    private void constructExpressionsAppearAtLocationMap() {
        expressionsAppearAtLocationMap = new HashMap<>();

        Set<LineLocation> locations = getAllLocationStatementMap().keySet();
        Map<Integer, LineLocation> lineNoToLocationMap = locations.stream().collect(Collectors.toMap(LineLocation::getLineNo, Function.identity()));

        ExpressionFromStatementCollector collector = new ExpressionFromStatementCollector();
        for (LineLocation location : getAllLocationStatementMap().keySet()) {
            Statement[] statements = new Statement[3];
            // collect also from lines directly above/below the current line
            statements[0] = getAllLocationStatementMap().get(location);
            statements[1] = getAllLocationStatementMap().getOrDefault(lineNoToLocationMap.get(location.getLineNo() - 1), null);
            statements[2] = getAllLocationStatementMap().getOrDefault(lineNoToLocationMap.get(location.getLineNo() + 1), null);

            collector.collect(statements);
            Set<ExpressionToMonitor> expressionToMonitorSet = collector.getExpressionsToMonitor();
            expressionsAppearAtLocationMap.put(location, expressionToMonitorSet);
        }
    }

    private Block getOriginalBodyBlock() {
        return originalBodyBlock;
    }

    private void constructLocationStatementMap() {
        StatementLocationCollector collector = new StatementLocationCollector(this.getContextMethod());
        Block block = getOriginalBodyBlock();
        collector.collectStatements(block);
        allLocationStatementMap = collector.getLineNoLocationMap();
        relevantLocationStatementMap = new HashMap<>(allLocationStatementMap);
        locationReturnStatementMap = new HashMap<>();
        for (LineLocation location : allLocationStatementMap.keySet()) {
            Statement statement = allLocationStatementMap.get(location);
            if (statement instanceof ReturnStatement) {
                locationReturnStatementMap.put(location, (ReturnStatement) statement);
            }
        }
    }
    //根据不相干语句，得到当前MethodDeclarationInfoCenter的相关语句，也就是要监听的语句
    //由于location和statement是没有对应的，所以要分开来删除
    public void buildReleventLocation(MethodDeclarationInfoCenter methodDeclarationInfoCenter){
        Set<LineLocation> locationToRemove = new HashSet<>();
        Set<Statement> irReleventStatements = methodDeclarationInfoCenter.getIrRelevantStatements();
        for(LineLocation lineLocation : relevantLocationStatementMap.keySet()){
            if(irReleventStatements.contains(relevantLocationStatementMap.get(lineLocation))){
                locationToRemove.add(lineLocation);
            }
        }
        for (LineLocation lineLocation : locationToRemove){
            relevantLocationStatementMap.remove(lineLocation);
        }
    }
    //在pruneIrrelevantLocation中添加了两条新语句
    public void pruneIrrelevantLocation(Set<LineLocation> relevantLocations) {
        Set<LineLocation> locationToRemove = new HashSet<>();
        Set<Statement> irReleventStatements = new HashSet<>();//新语句
        for (LineLocation line : relevantLocationStatementMap.keySet()) {
            if (!relevantLocations.contains(line)) {
                locationToRemove.add(line);
            }
        }
        for (LineLocation location : locationToRemove) {
            irReleventStatements.add(location.getStatement());//新语句
            relevantLocationStatementMap.remove(location);
        }
        this.setIrRelevantStatements(irReleventStatements);
    }

    private void constructVarDefAssignMap() {
        LocalVariableDefAssignCollector collector = new LocalVariableDefAssignCollector();
        collector.collect(getContextMethod());
        variableDefinitionLocationMap = collector.getVariableDefinitionLocationMap();

        Map<IVariableBinding, LineLocation> assignmentLocations = collector.getVariableAssignmentLocationMap();
        variableAssignmentLocationMap = new HashMap<>();
        for (IVariableBinding var : assignmentLocations.keySet()) {
            LineLocation firstAssignLoc = assignmentLocations.get(var);
            LineScope scope = variableDefinitionLocationMap.get(var);
            LineScope writeScope = new LineScope(firstAssignLoc, scope.getEndLocation());
            variableAssignmentLocationMap.put(var, writeScope);
        }
    }

    public Map<String, ITypeBinding> getExpressionTextToTypeMap() {
        if (expressionTextToTypeMap == null) {
            expressionTextToTypeMap = new HashMap<>();
        }
        return expressionTextToTypeMap;
    }

    public Map<String, ITypeBinding> getAllTypeBindingMap() {
        if (allTypeBindingMap == null) allTypeBindingMap = new HashMap<>();
        return allTypeBindingMap;
    }

    public Map<String, ExpressionToMonitor> getAllExpressionToMonitorMap() {
        if (allExpressionToMonitorMap == null)
            allExpressionToMonitorMap = new HashMap<>();
        return allExpressionToMonitorMap;
    }

    public ITypeBinding getTypeByExpressionText(String text) {
        return getExpressionTextToTypeMap().getOrDefault(text.trim(), null);
    }

    public ExpressionToMonitor getExpressionByText(String etmText) {
        return getAllExpressionToMonitorMap().get(etmText.trim());
    }

    public ExpressionToMonitor getExpressionByText(String etmText, String typeText, boolean createNewETM) {
        ExpressionToMonitor etm = getExpressionByText(etmText);
        if (etm == null) {
            if (createNewETM) {
                ITypeBinding typeBinding = getTypeByExpressionText(etmText);
                if (typeBinding == null)
                    typeBinding = getAllTypeBindingMap().get(typeText);
                if (typeBinding == null)
                    typeBinding = getThisExpressionToMonitor().getExpressionAST().getAST().resolveWellKnownType(typeText);
                if (typeBinding != null)// ignore expressions with non-primitive type that not refereed by MTF exp
                    etm = ExpressionToMonitor.construct(newAstExpression(etmText), typeBinding);
                return etm;
            }
        } else {
            if (etm.getType().getErasure().getQualifiedName().equals(typeText)) return etm;
        }
        return null;
    }


    public boolean hasExpressionTextRegistered(String text) {
        return getExpressionTextToTypeMap().containsKey(text);
    }

    public void registerExpressionToMonitor(ExpressionToMonitor expressionToMonitor) {
        if (!getExpressionTextToTypeMap().containsKey(expressionToMonitor.getText().trim()))
            getExpressionTextToTypeMap().put(expressionToMonitor.getText(), expressionToMonitor.getType());
        if (!getAllExpressionToMonitorMap().containsKey(expressionToMonitor.getText().trim()))
            getAllExpressionToMonitorMap().put(expressionToMonitor.getText(), expressionToMonitor);
        if (!getAllTypeBindingMap().containsKey(expressionToMonitor.getType().getQualifiedName()))
            getAllTypeBindingMap().put(expressionToMonitor.getType().getQualifiedName().trim(), expressionToMonitor.getType());
    }

    private void collectExpressionsToMonitor() {
        Set<ExpressionToMonitor> expressionToMonitorSet = new HashSet<>();

        // Collect sub-expressions from source code
        ExpressionCollector expressionCollector = new ExpressionCollector(true);
        expressionCollector.collect(getMethodDeclaration());
        expressionCollector.getSubExpressionSet().stream()
                .filter(x -> !(x instanceof NumberLiteral))
                .forEach(x -> expressionToMonitorSet.add(ExpressionToMonitor.construct(x, x.resolveTypeBinding())));
        if (!isStaticMtf())
            expressionToMonitorSet.add(getThisExpressionToMonitor());

        expressionToMonitorSet.addAll(basicEnrich(expressionToMonitorSet.stream()
                .filter(x -> !x.getText().contains(Tester.JAID_KEY_WORD))
                .collect(Collectors.toSet())));

        //Recording and enriching ETM
        getBasicExpressions().addAll(expressionToMonitorSet.stream()
                .filter(x -> !x.getText().contains(Tester.JAID_KEY_WORD))
                .collect(Collectors.toSet())
        );
        getAllEnrichingEtmWithinMethod().addAll(enrichExpressionsInAllKinds(getBasicExpressions()));
        getBooleanEnrichingEtmWithinMethod().addAll(enrichExpressionsReturnBoolean(getBasicExpressions()));
        getReferenceEtmFields().addAll(extendReferenceFields(getBasicExpressions()));
        // Construct method invocation with collected fields (Please use all files while parsing AST to correctly enable the construction).
        // getAllEnrichingEtmWithinMethod().addAll(enrichExpressionsReturnBoolean(getReferenceEtmFields()));

        // collect integer literals
        constantIntegers = new HashSet<>();
        expressionCollector.getSubExpressionSet().stream().filter(x -> x instanceof NumberLiteral)
                .forEach(x -> constantIntegers.add(ExpressionToMonitor.construct(x, x.resolveTypeBinding())));
        constantIntegers.addAll(enrichConstantIntegers(getMethodDeclaration().getAST()));

        if (shouldLogDebug()) {
            LoggingService.debugFileOnly("============ Expressions To Monitor Within Method (total number: " + getAllEnrichedEtmWithinMethod().size() + ")", MONITORED_EXPS);
            getAllEnrichedEtmWithinMethod().forEach(f -> LoggingService.debugFileOnly(f.getType().getQualifiedName() + " :: " + f.toString(), MONITORED_EXPS));

            LoggingService.debugFileOnly("============ Constant Integers (total number: " + getConstantIntegers().size() + ")", MONITORED_EXPS);
            getConstantIntegers().forEach(f -> LoggingService.debugFileOnly(f.getType().getQualifiedName() + " :: " + f.toString(), MONITORED_EXPS));
        }
    }
    private void recordEntryAndExitLocation_Patch(){
        CompilationUnit compilationUnit = (CompilationUnit) getMethodDeclaration().getRoot();
        Statement entryStatement = (Statement) (getMethodDeclaration().getBody().statements().get(ClassToFixPreprocessor_Patch.getEntryStatementIndex()));
        Statement exitStatement = (Statement) (((TryStatement) getMethodDeclaration().getBody().statements().get(ClassToFixPreprocessor_Patch.getTryStatementIndex()))
                .getFinally().statements().get(1));

        entryLocation = LineLocation.newLineLocation(getContextMethod(), compilationUnit.getLineNumber(entryStatement.getStartPosition()));
        exitLocation =  LineLocation.newLineLocation(getContextMethod(), compilationUnit.getLineNumber(exitStatement.getStartPosition()));
    }
    private void recordEntryAndExitLocation() {
        CompilationUnit compilationUnit = (CompilationUnit) getMethodDeclaration().getRoot();
        Statement entryStatement = (Statement) (getMethodDeclaration().getBody().statements().get(ClassToFixPreprocessor.getEntryStatementIndex()));
        Statement exitStatement = (Statement) (((TryStatement) getMethodDeclaration().getBody().statements().get(ClassToFixPreprocessor.getTryStatementIndex()))
                .getFinally().statements().get(1));
        entryLocation = LineLocation.newLineLocation(getContextMethod(), compilationUnit.getLineNumber(entryStatement.getStartPosition()));
        exitLocation =  LineLocation.newLineLocation(getContextMethod(), compilationUnit.getLineNumber(exitStatement.getStartPosition()));
    }

    //模仿这个方法来实现第一步
    public void mappingObservedStatesToSnapshots(TestExecutionResult testExecutionResult) {
        Set<LineLocation> relatedLocation = new HashSet<>();
        Set<StateSnapshot> relatedStateSnapshots = new HashSet<>();

        for (ProgramState oneObservedState : testExecutionResult.getObservedStates()) {
            LineLocation location = oneObservedState.getLocation();
            relatedLocation.add(location);

            if (!getRelevantLocationStatementMap().containsKey(location))
                continue;
            for (StateSnapshotExpression expressionToMonitor : getStateSnapshotExpressionsWithinMethod()) {
                DebuggerEvaluationResult evaluationResult = expressionToMonitor.evaluate(oneObservedState);
                if (evaluationResult.hasSemanticError() || evaluationResult.hasSyntaxError())
                    continue;
                if (!(evaluationResult instanceof DebuggerEvaluationResult.BooleanDebuggerEvaluationResult))
                    throw new IllegalStateException();
                boolean booleanEvaluationResult = ((DebuggerEvaluationResult.BooleanDebuggerEvaluationResult) evaluationResult).getValue();

                //最重要的一句
                StateSnapshot stateSnapshot = getStateSnapshot(location, expressionToMonitor, booleanEvaluationResult);
                if (stateSnapshot != null) {//if snapshot is null means it is not valid at that location
                    relatedStateSnapshots.add(stateSnapshot);
                    if (testExecutionResult.wasSuccessful()){
                        stateSnapshot.increaseOccurrenceInPassing();
                    }
                    else{
                        stateSnapshot.increaseOccurrenceInFailing();
                    }

                }
            }
        }
        // record location coverage information
        for (LineLocation lineLocation : relatedLocation) {
            if (testExecutionResult.wasSuccessful())
                lineLocation.increasingOccurrenceInPassing();
            else
                lineLocation.increasingOccurrenceInFailing();
        }
        // record state-snapshot coverage (no-duplication) information
        for (StateSnapshot relatedStateSnapshot : relatedStateSnapshots) {
            if (testExecutionResult.wasSuccessful())
                relatedStateSnapshot.increaseOccurrenceInPassingNoDup();
            else
                relatedStateSnapshot.increaseOccurrenceInFailingNoDup();
        }
    }


    public void buildStateSnapshotsWithinMethod() {

//        buildStateSnapshotExpressionsWithinMethod();采用这个方法会导致无法挖掘更深的表达式
        //但是用了下面的表达式则会将更深层次的表达式挖掘出来，
        buildStateSnapshotExpressionsWithinMethodWithAllEnriched();

        categorizedStateSnapshotsWithinMethod = new HashMap<>();
        stateSnapshotsWithinMethod = new HashSet<>();

        for (LineLocation location : locationExpressionMap.keySet()) {
            if (!getRelevantLocationStatementMap().containsKey(location))
                continue;
            //这里叫subMap是因为少了一个location
            Map<StateSnapshotExpression, Map<Boolean, StateSnapshot>> subMap = new HashMap<>();
            categorizedStateSnapshotsWithinMethod.put(location, subMap);

            for (StateSnapshotExpression stateSnapshotExpression : stateSnapshotExpressionsWithinMethod) {
                Map<Boolean, StateSnapshot> subsubMap = new HashMap<>();
                subMap.put(stateSnapshotExpression, subsubMap);
                if (stateSnapshotExpression.isValidAtLocation(location, locationExpressionMap, this)) {
                    StateSnapshot stateSnapshotTrue = new StateSnapshot(location, stateSnapshotExpression, DebuggerEvaluationResult.BooleanDebuggerEvaluationResult.getBooleanDebugValue(true));
                    StateSnapshot stateSnapshotFalse = new StateSnapshot(location, stateSnapshotExpression, DebuggerEvaluationResult.BooleanDebuggerEvaluationResult.getBooleanDebugValue(false));
                    subsubMap.put(true, stateSnapshotTrue);
                    subsubMap.put(false, stateSnapshotFalse);

                    stateSnapshotsWithinMethod.add(stateSnapshotTrue);
                    stateSnapshotsWithinMethod.add(stateSnapshotFalse);
                }
            }
        }
    }

    private void buildStateSnapshotExpressionsWithinMethod() {
        StateSnapshotExpressionBuilder builder = new StateSnapshotExpressionBuilder();
        builder.build(getBasicExpressions().stream().filter(ExpressionToMonitor::isSideEffectFree).collect(Collectors.toList()),
                new ArrayList<>(getConstantIntegers()));
        builder.addBooleanETM(getAllEnrichingEtmWithinMethod());//Adding enriching ETM that invoke methods that return boolean.
        builder.BuildEnrichedReferenceETM(getAllEnrichingEtmWithinMethod());//Building snapshots for enriching ETM that invoke methods that return referenceType.
        stateSnapshotExpressionsWithinMethod = builder.getStateSnapshotExpressions();
        LoggingService.infoFileOnly("stateSnapshotExpressionsWithinMethod size:" + stateSnapshotExpressionsWithinMethod.size(), FixerOutput.LogFile.MONITORED_EXPS);
    }

    private void buildStateSnapshotExpressionsWithinMethodWithAllEnriched() {
        StateSnapshotExpressionBuilder builder = new StateSnapshotExpressionBuilder();
        builder.build(getAllEnrichedEtmWithinMethod().stream().filter(ExpressionToMonitor::isSideEffectFree).collect(Collectors.toList()),
                new ArrayList<>(getConstantIntegers()));
        stateSnapshotExpressionsWithinMethod = builder.getStateSnapshotExpressions();
        LoggingService.infoFileOnly("stateSnapshotExpressionsWithinMethod size:" + stateSnapshotExpressionsWithinMethod.size(), FixerOutput.LogFile.MONITORED_EXPS);
    }


    public void retainSideEffectFreeExpressionsToLocation() {
        Map newLocationExpressionMap = new HashMap<>();
        for (LineLocation line : relevantLocationStatementMap.keySet()) {
            SortedSet<ExpressionToMonitor> sideEffectFreeExpressions = new TreeSet<>(ExpressionToMonitor.getByLengthComparator());
            locationExpressionMap.get(line).stream().filter(ExpressionToMonitor::isSideEffectFree).forEach(sideEffectFreeExpressions::add);
            newLocationExpressionMap.put(line, sideEffectFreeExpressions);
        }
        locationExpressionMap = newLocationExpressionMap;
    }

    public void registerExpressionsToLocation() {
        locationExpressionMap = new HashMap<>();
        SortedSet<ExpressionToMonitor> expressionsToMonitorWithinMethod = getAllEnrichedEtmWithinMethod();
        for (LineLocation location : getAllLocationStatementMap().keySet()) {
            locationExpressionMap.put(location, new TreeSet<>(expressionsToMonitorWithinMethod));
        }
    }

    // ============================== Storage

    private Block originalBodyBlock;
    private LineLocation entryLocation;
    private LineLocation exitLocation;
    private ExpressionToMonitor thisExpressionToMonitor;
    private ExpressionToMonitor resultExpressionToMonitor;

    private Map<IVariableBinding, LineScope> variableDefinitionLocationMap;
    private Map<IVariableBinding, LineScope> variableAssignmentLocationMap;

    //Locations
    private Map<LineLocation, Statement> relevantLocationStatementMap;//locations exe by failing test
    private Map<LineLocation, Statement> allLocationStatementMap;//all locations
    private Map<LineLocation, ReturnStatement> locationReturnStatementMap;
    private Map<LineLocation, Double> locationDistanceToFailureMap;
    private Map<LineLocation, SortedSet<ExpressionToMonitor>> locationExpressionMap;//[monitor-related] valid expressions in each location
    private Map<LineLocation, Set<ExpressionToMonitor>> expressionsAppearAtLocationMap;// expression appear at or near the location
    private Map<LineLocation, Set<ExpressionToMonitor>> expressionsToMonitorNearLocation;// expression appear at or near the location

    //Enriched ETM
    private SortedSet<ExpressionToMonitor> basicExpressions;//[SS-related] EXPs appear in MTF (existingEXP) + existingEXP.size()+ existingEXP.length
    private SortedSet<ExpressionToMonitor> allEnrichingExpressionsToMonitorWithinMethod;//[SS-related]  basicEXP.getStateMethod
    private SortedSet<ExpressionToMonitor> booleanEnrichingExpressionsToMonitorWithinMethod;//[SS-related] basicEXP.getStateMethodReturnBoolean
    private SortedSet<ExpressionToMonitor> referenceFields;//[SS-related] basicEXP.fields
    private SortedSet<ExpressionToMonitor> ExpressionsToMonitorWithSideEffect;//[SS-related]

    //Registered ETM
    private Map<String, ITypeBinding> expressionTextToTypeMap;
    private Map<String, ITypeBinding> allTypeBindingMap;
    private Map<String, ExpressionToMonitor> allExpressionToMonitorMap;
    private Set<ExpressionToMonitor> constantIntegers;

    //Snapshots
    private Set<StateSnapshotExpression> stateSnapshotExpressionsWithinMethod;
    private Map<LineLocation, Map<StateSnapshotExpression, Map<Boolean, StateSnapshot>>> categorizedStateSnapshotsWithinMethod;
    private Set<StateSnapshot> stateSnapshotsWithinMethod;

    private Set<Statement> irRelevantStatements;


}
