import { request } from '@/api/http';
import type { PageResult } from '@/types/api';

export interface PostSummary {
  id: number;
  title: string;
  slug: string;
  summary?: string;
  coverUrl?: string;
  publishedAt?: string;
  authorName?: string;
  tagNames?: string[];
}

export interface PostDetail extends PostSummary {
  content: string;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
}

export const postApi = {
  fetchPosts(params: Record<string, unknown> = {}) {
    return request<PageResult<PostSummary>>({
      url: '/posts',
      method: 'GET',
      params
    });
  },
  fetchPostDetail(slug: string) {
    return request<PostDetail>({
      url: `/posts/${slug}`,
      method: 'GET'
    });
  }
};
