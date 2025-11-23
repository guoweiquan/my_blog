import { request } from '@/api/http';
import type { AnalyticsOverview } from '@/types/analytics';

export const analyticsApi = {
  fetchOverview() {
    return request<AnalyticsOverview>({
      url: '/admin/analytics/overview',
      method: 'GET'
    });
  }
};
