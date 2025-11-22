import { defineStore } from 'pinia';
import { postApi, type PostDetail, type PostSummary } from '@/api/modules/post';
import type { PageResult } from '@/types/api';

interface PostState {
  list: PostSummary[];
  pagination: Pick<PageResult<PostSummary>, 'page' | 'size' | 'total'>;
  loading: boolean;
  currentPost: PostDetail | null;
}

export const usePostStore = defineStore('post', {
  state: (): PostState => ({
    list: [],
    pagination: { page: 1, size: 10, total: 0 },
    loading: false,
    currentPost: null
  }),
  actions: {
    async fetchPosts(params: Record<string, unknown> = {}) {
      this.loading = true;
      try {
        const data = await postApi.fetchPosts({ page: this.pagination.page, size: this.pagination.size, ...params });
        this.list = data.records;
        this.pagination = {
          page: data.page,
          size: data.size,
          total: data.total
        };
      } finally {
        this.loading = false;
      }
    },
    async fetchPostDetail(slug: string) {
      this.currentPost = await postApi.fetchPostDetail(slug);
    }
  }
});
