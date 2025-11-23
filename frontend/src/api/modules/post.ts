import { request } from '@/api/http';
import type { PageResult } from '@/types/api';
import type { PostDetail, PostPayload, PostSummary } from '@/types/post';

export const postApi = {
  fetchPosts(params: Record<string, unknown> = {}) {
    return request<PageResult<PostSummary>>({
      url: '/posts',
      method: 'GET',
      params
    });
  },
  fetchManagePosts(params: Record<string, unknown> = {}) {
    return request<PageResult<PostSummary>>({
      url: '/posts/manage',
      method: 'GET',
      params
    });
  },
  fetchPostDetail(slug: string) {
    return request<PostDetail>({
      url: `/posts/${slug}`,
      method: 'GET'
    });
  },
  createPost(payload: PostPayload) {
    return request<PostDetail>({
      url: '/posts',
      method: 'POST',
      data: payload
    });
  },
  updatePost(id: number, payload: PostPayload) {
    return request<PostDetail>({
      url: `/posts/${id}`,
      method: 'PUT',
      data: payload
    });
  },
  deletePost(id: number) {
    return request<void>({
      url: `/posts/${id}`,
      method: 'DELETE'
    });
  },
  searchPosts(params: { q: string; page?: number; size?: number }) {
    return request<PageResult<PostSummary>>({
      url: '/search/posts',
      method: 'GET',
      params
    });
  }
};
