export interface AnalyticsOverview {
  todayPv: number;
  todayUv: number;
  publishedPosts: number;
  pendingComments: number;
  hotPosts: HotPost[];
}

export interface HotPost {
  postId: number;
  title: string;
  slug: string;
  viewCount?: number;
  score: number;
}
