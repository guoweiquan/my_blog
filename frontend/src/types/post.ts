export interface PostSummary {
  id: number;
  title: string;
  slug: string;
  summary?: string;
  coverUrl?: string;
  status: string;
  authorName?: string;
  publishedAt?: string;
  tagNames?: string[];
  viewCount?: number;
  likeCount?: number;
}

export interface PostDetail extends PostSummary {
  content: string;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  likedByCurrentUser?: boolean;
  favoritedByCurrentUser?: boolean;
}

export interface PostPayload {
  title: string;
  slug: string;
  summary?: string;
  content: string;
  coverUrl?: string;
  status: string;
  readingTime?: number;
  seoKeywords?: string;
  tagIds?: number[];
}
