<template>
  <article class="post-detail" v-if="post">
    <header>
      <p class="meta">{{ post.authorName || '管理员' }} · {{ post.publishedAt || '草稿' }}</p>
      <h1>{{ post.title }}</h1>
      <div class="tags" v-if="post.tagNames?.length">
        <el-tag v-for="tag in post.tagNames" :key="tag" size="small">{{ tag }}</el-tag>
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
        <p class="current-user">以 {{ displayName }} 的身份发表评论</p>
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="说点什么吧..."
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
        <li v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <span class="author">{{ comment.authorName }}</span>
            <span class="date">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <p class="comment-content">{{ comment.content }}</p>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { usePostStore } from '@/stores/post';
import { useAuthStore } from '@/stores/auth';
import { commentApi } from '@/api/modules/comment';
import type { CommentItem } from '@/types/comment';

const route = useRoute();
const postStore = usePostStore();
const authStore = useAuthStore();

const comments = ref<CommentItem[]>([]);
const commentsLoading = ref(false);
const commentContent = ref('');
const submittingComment = ref(false);

const slug = computed(() => route.params.slug as string);
const post = computed(() => postStore.currentPost);
const isLoading = computed(() => postStore.loading);
const postId = computed(() => post.value?.id ?? null);

const isAuthenticated = computed(() => authStore.isAuthenticated);
const displayName = computed(() => authStore.displayName);

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

const submitComment = async () => {
  if (!isAuthenticated.value) {
    return ElMessage.warning('请先登录');
  }
  if (!postId.value) return;
  const content = commentContent.value.trim();
  if (!content) {
    return ElMessage.warning('请输入评论内容');
  }
  submittingComment.value = true;
  try {
    await commentApi.createComment(postId.value, { content });
    commentContent.value = '';
    ElMessage.success('评论发布成功');
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

onMounted(loadPost);
watch(slug, () => {
  loadPost();
});
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

.comment-item {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #909399;
}

.comment-content {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
