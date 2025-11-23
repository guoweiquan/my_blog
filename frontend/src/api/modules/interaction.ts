import { request } from '@/api/http';

export interface InteractionResult {
  active: boolean;
  total: number;
}

export interface SubscriptionResult {
  subscribed: boolean;
}

export const interactionApi = {
  toggleLike(postId: number) {
    return request<InteractionResult>({
      url: `/posts/${postId}/like`,
      method: 'POST'
    });
  },
  toggleFavorite(postId: number) {
    return request<InteractionResult>({
      url: `/posts/${postId}/favorite`,
      method: 'POST'
    });
  },
  toggleTagSubscription(tagId: number) {
    return request<SubscriptionResult>({
      url: `/tags/${tagId}/subscribe`,
      method: 'POST'
    });
  }
};
