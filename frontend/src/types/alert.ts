export interface AlertItem {
  id: number;
  userId: number;
  userName: string;
  ruleCode: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  hitTime: string;
  status: 'OPEN' | 'CLOSED';
  description: string;
  suspiciousProblems: string;
  suggestion: string;
  mailSentAt: string | null;
  studentFeedback: string | null;
  feedbackAt: string | null;
}

export interface UpdateAlertFeedbackPayload {
  feedback: string | null;
}
