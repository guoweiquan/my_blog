import { request } from '@/api/http';
import type { CommentItem, CommentPayload } from '@/types/comment';

export const commentApi = {
  fetchComments(postId: number) {
    return request<CommentItem[]>({
      url: `/posts/${postId}/comments`,
      method: 'GET'
    });
  },
  createComment(postId: number, payload: CommentPayload) {
    return request<CommentItem>({
      url: `/posts/${postId}/comments`,
      method: 'POST',
      data: payload
    });
  }
};
