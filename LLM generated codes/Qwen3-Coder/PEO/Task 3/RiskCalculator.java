public class RiskCalculator {
    
    /**
     * Calculates risk based on balance and credit limit
     * @param account The financial account to analyze
     * @return Risk metric (0.0 = low risk, 10.0 = high risk)
     */
    public static double calculateAccountRisk(FinancialAccount account) {
        return account.calculateRiskMetric();
    }
    
    /**
     * Calculates risk with custom weights
     * @param account The financial account to analyze
     * @param balanceWeight Weight for balance component
     * @param creditLimitWeight Weight for credit limit component
     * @return Risk metric (0.0 = low risk, 10.0 = high risk)
     */
    public static double calculateCustomRisk(FinancialAccount account, 
                                           double balanceWeight, 
                                           double creditLimitWeight) {
        return account.calculateRiskMetricWithWeight(balanceWeight, creditLimitWeight);
    }
    
    /**
     * Interprets risk level
     * @param riskMetric The calculated risk metric
     * @return Risk level description
     */
    public static String interpretRiskLevel(double riskMetric) {
        if (riskMetric <= 2.0) {
            return "Low Risk";
        } else if (riskMetric <= 5.0) {
            return "Medium Risk";
        } else if (riskMetric <= 8.0) {
            return "High Risk";
        } else {
            return "Very High Risk";
        }
    }
}