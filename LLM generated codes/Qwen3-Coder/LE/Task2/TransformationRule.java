/**
 * Represents a single transformation rule
 */
public class TransformationRule {
    private String ruleId;
    private String sourceField;
    private String targetField;
    private String operation;
    private Object parameter;
    
    public TransformationRule(String ruleId, String sourceField, String targetField, String operation, Object parameter) {
        this.ruleId = ruleId;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.operation = operation;
        this.parameter = parameter;
    }
    
    public String getRuleId() { return ruleId; }
    public String getSourceField() { return sourceField; }
    public String getTargetField() { return targetField; }
    public String getOperation() { return operation; }
    public Object getParameter() { return parameter; }
}