import { request } from '@/api/http';
import type { TagItem } from '@/types/tag';

export const tagApi = {
  fetchAll() {
    return request<TagItem[]>({
      url: '/tags',
      method: 'GET'
    });
  },
  createTag(payload: { name: string; description?: string }) {
    return request<TagItem>({
      url: '/tags',
      method: 'POST',
      data: payload
    });
  },
  updateTag(id: number, payload: { name: string; description?: string }) {
    return request<TagItem>({
      url: `/tags/${id}`,
      method: 'PUT',
      data: payload
    });
  },
  deleteTag(id: number) {
    return request<void>({
      url: `/tags/${id}`,
      method: 'DELETE'
    });
  }
};
