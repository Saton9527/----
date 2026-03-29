export interface AlertItem {
  id: number;
  userName: string;
  ruleCode: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  hitTime: string;
  status: 'OPEN' | 'CLOSED';
  description: string;
  suspiciousProblems: string;
  suggestion: string;
}
