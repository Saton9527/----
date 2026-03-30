export interface ContestItem {
  id: number;
  platform: string;
  sourceType: 'MANUAL' | 'OFFICIAL';
  title: string;
  url: string;
  startTime: string;
  reminderTime: string;
  reminderMinutes: number;
  status: 'TODAY' | 'UPCOMING' | 'FINISHED';
}
