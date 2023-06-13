package hk.polyu.comp.jaid.monitor;

import hk.polyu.comp.jaid.ast.ExpressionFormatter;
import hk.polyu.comp.jaid.util.CommonUtils;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Created by Ls CHEN.
 */
public class OriginalETM extends ExpressionToMonitor {

    public static ExpressionToMonitor construct(Expression expressionAST, ITypeBinding type) {
        //use existing ETM if there is any
        Expression formattedExp = ExpressionFormatter.formatExpression(expressionAST);
        ExpressionToMonitor etm = infoCenter.getExpressionByText(getText(formattedExp.toString()));
        if (etm != null) return etm;
        //create originalETM
        OriginalETM originalETM = new OriginalETM(formattedExp, type);
        originalETM.setOriginalExpressionAST(expressionAST);
        originalETM.setBinding(CommonUtils.resolveBinding4Variables(expressionAST));
        //register new ETM
        infoCenter.registerExpressionToMonitor(originalETM);
        return originalETM;
    }

    private Expression originalExpressionAST;

    public OriginalETM(Expression expressionAST, ITypeBinding type) {
        super(expressionAST, type);
    }

    public void setOriginalExpressionAST(Expression originalExpressionAST) {
        this.originalExpressionAST = originalExpressionAST;
    }

    @Override
    public String toString() {
        return new StringBuilder("OriginalETM:").append(getText()).append(" [").append(type.getName().toString()).append("]").toString();
    }
}
