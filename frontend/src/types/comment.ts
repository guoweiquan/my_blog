export interface CommentItem {
  id: number;
  postId: number;
  parentId?: number;
  content: string;
  authorName: string;
  createdAt: string;
}

export interface CommentPayload {
  content: string;
  parentId?: number;
}
