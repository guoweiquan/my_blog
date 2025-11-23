export interface CommentItem {
  id: number;
  postId: number;
  parentId?: number;
  content: string;
  authorName: string;
  createdAt: string;
  children?: CommentItem[];
}

export interface CommentPayload {
  content: string;
  parentId?: number;
}

export interface CommentModerationItem {
  id: number;
  postId: number;
  postTitle: string;
  authorName: string;
  content: string;
  status: string;
  createdAt: string;
}
