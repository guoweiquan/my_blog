<template>
  <article class="post-detail" v-if="post">
    <header>
      <p class="meta">
        <span>{{ post.authorName || '管理员' }}</span>
        <span>·</span>
        <span>{{ formatDate(post.publishedAt) || '草稿' }}</span>
      </p>
      <h1>{{ post.title }}</h1>
      <div class="tags" v-if="post.tagNames?.length">
        <el-tag v-for="tag in post.tagNames" :key="tag" size="small">{{ tag }}</el-tag>
      </div>
      <div class="stats">
        <span><el-icon><View /></el-icon>{{ post.viewCount ?? 0 }} 阅读</span>
        <span><el-icon><ChatDotRound /></el-icon>{{ post.commentCount ?? 0 }} 评论</span>
      </div>
      <div class="post-actions">
        <el-button
          :type="post.likedByCurrentUser ? 'primary' : 'default'"
          :plain="!post.likedByCurrentUser"
          :loading="likeLoading"
          @click="toggleLike"
        >
          <el-icon><Pointer /></el-icon>
          {{ post.likeCount ?? 0 }} 赞
        </el-button>
        <el-button
          :type="post.favoritedByCurrentUser ? 'success' : 'default'"
          :plain="!post.favoritedByCurrentUser"
          :loading="favoriteLoading"
          @click="toggleFavorite"
        >
          <el-icon>
            <StarFilled v-if="post.favoritedByCurrentUser" />
            <Star v-else />
          </el-icon>
          {{ post.favoritedByCurrentUser ? '已收藏' : '收藏' }}
        </el-button>
      </div>
    </header>
    <section class="content" v-html="post.content" />
  </article>
  <el-skeleton v-else-if="isLoading" animated :rows="10" />
  <el-empty v-else description="文章不存在或已删除" />

  <section v-if="post" class="comment-section">
    <h3>评论区</h3>
    <el-card class="comment-card">
      <template v-if="isAuthenticated">
        <div v-if="replyingTo" class="reply-chip">
          正在回复 <strong>{{ replyingTo.authorName }}</strong>
          <el-button link type="primary" @click="cancelReply">取消</el-button>
        </div>
        <p class="current-user">以 {{ displayName }} 的身份发表评论</p>
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          :placeholder="commentPlaceholder"
        />
        <div class="comment-actions">
          <el-button type="primary" :loading="submittingComment" @click="submitComment">发布评论</el-button>
        </div>
      </template>
      <template v-else>
        <p class="login-tip">
          登录后才能发表评论，<RouterLink to="/login">立即登录</RouterLink>
        </p>
      </template>
    </el-card>

    <div class="comment-list">
      <el-skeleton v-if="commentsLoading" animated :rows="4" />
      <el-empty v-else-if="!comments.length" description="暂无评论，快来抢沙发" />
      <ul v-else>
        <CommentItemNode
          v-for="comment in comments"
          :key="comment.id"
          :comment="comment"
          @reply="handleReply"
        />
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ChatDotRound, Pointer, Star, StarFilled, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import CommentItemNode from '@/components/comment/CommentItemNode.vue';
import { commentApi } from '@/api/modules/comment';
import { interactionApi } from '@/api/modules/interaction';
import { useAuthStore } from '@/stores/auth';
import { usePostStore } from '@/stores/post';
import type { CommentItem } from '@/types/comment';

const route = useRoute();
const postStore = usePostStore();
const authStore = useAuthStore();

const comments = ref<CommentItem[]>([]);
const commentsLoading = ref(false);
const commentContent = ref('');
const submittingComment = ref(false);
const replyingTo = ref<CommentItem | null>(null);
const likeLoading = ref(false);
const favoriteLoading = ref(false);

const slug = computed(() => route.params.slug as string);
const post = computed(() => postStore.currentPost);
const isLoading = computed(() => postStore.loading);
const postId = computed(() => post.value?.id ?? null);

const isAuthenticated = computed(() => authStore.isAuthenticated);
const displayName = computed(() => authStore.displayName);
const commentPlaceholder = computed(() =>
  replyingTo.value ? `回复 ${replyingTo.value.authorName}...` : '说点什么吧...'
);

const loadPost = async () => {
  if (!slug.value) return;
  try {
    await postStore.fetchPostDetail(slug.value);
  } catch (error: any) {
    ElMessage.error(error?.message || '文章加载失败');
  }
};

const loadComments = async (id: number) => {
  commentsLoading.value = true;
  try {
    comments.value = await commentApi.fetchComments(id);
  } catch (error: any) {
    ElMessage.error(error?.message || '评论加载失败');
  } finally {
    commentsLoading.value = false;
  }
};

const ensureAuthenticated = () => {
  if (!isAuthenticated.value) {
    ElMessage.warning('请先登录');
    return false;
  }
  return true;
};

const toggleLike = async () => {
  if (!postId.value || !ensureAuthenticated()) return;
  likeLoading.value = true;
  try {
    const result = await interactionApi.toggleLike(postId.value);
    if (postStore.currentPost) {
      postStore.currentPost.likeCount = result.total;
      postStore.currentPost.likedByCurrentUser = result.active;
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败');
  } finally {
    likeLoading.value = false;
  }
};

const toggleFavorite = async () => {
  if (!postId.value || !ensureAuthenticated()) return;
  favoriteLoading.value = true;
  try {
    const result = await interactionApi.toggleFavorite(postId.value);
    if (postStore.currentPost) {
      postStore.currentPost.favoritedByCurrentUser = result.active;
    }
    ElMessage.success(result.active ? '已收藏' : '已取消收藏');
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败');
  } finally {
    favoriteLoading.value = false;
  }
};

const handleReply = (comment: CommentItem) => {
  replyingTo.value = comment;
};

const cancelReply = () => {
  replyingTo.value = null;
};

const submitComment = async () => {
  if (!postId.value || !ensureAuthenticated()) return;
  const content = commentContent.value.trim();
  if (!content) {
    return ElMessage.warning('请输入评论内容');
  }
  submittingComment.value = true;
  try {
    await commentApi.createComment(postId.value, {
      content,
      parentId: replyingTo.value?.id
    });
    commentContent.value = '';
    replyingTo.value = null;
    ElMessage.success('评论发布成功，待审核通过后可见');
    await loadComments(postId.value);
  } catch (error: any) {
    ElMessage.error(error?.message || '评论发布失败');
  } finally {
    submittingComment.value = false;
  }
};

const formatDate = (value?: string) => {
  if (!value) return '';
  return new Date(value).toLocaleString();
};

watch(
  () => slug.value,
  () => {
    loadPost();
    replyingTo.value = null;
    commentContent.value = '';
  },
  { immediate: true }
);

watch(postId, (id) => {
  if (id) {
    loadComments(id);
  } else {
    comments.value = [];
  }
});
</script>

<style scoped>
.post-detail {
  background: #fff;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  margin-bottom: 32px;
}

.meta {
  color: #909399;
  margin-bottom: 8px;
  display: flex;
  gap: 6px;
}

.tags {
  margin: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stats {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}

.stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.post-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.content {
  margin-top: 24px;
  line-height: 1.8;
  color: #333;
}

.content :deep(pre) {
  background: #1e1e1e;
  color: #f5f5f5;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
}

.comment-section {
  margin-top: 24px;
}

.comment-card {
  margin-bottom: 24px;
}

.current-user {
  margin-bottom: 8px;
  color: #606266;
}

.comment-actions {
  margin-top: 12px;
  text-align: right;
}

.login-tip {
  color: #909399;
}

.comment-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reply-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ecf5ff;
  color: #409eff;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}
</style>
