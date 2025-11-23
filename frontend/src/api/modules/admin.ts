import { request } from '@/api/http';
import type { PageResult } from '@/types/api';
import type { CommentModerationItem } from '@/types/comment';

export const adminApi = {
  fetchComments(params: { status?: string; page?: number; size?: number }) {
    return request<PageResult<CommentModerationItem>>({
      url: '/admin/comments',
      method: 'GET',
      params
    });
  },
  approveComment(id: number) {
    return request<void>({
      url: `/admin/comments/${id}/approve`,
      method: 'PUT'
    });
  },
  rejectComment(id: number) {
    return request<void>({
      url: `/admin/comments/${id}/reject`,
      method: 'PUT'
    });
  },
  deleteComment(id: number) {
    return request<void>({
      url: `/admin/comments/${id}`,
      method: 'DELETE'
    });
  }
};
