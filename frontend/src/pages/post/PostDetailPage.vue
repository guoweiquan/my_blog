<template>
  <article class="post-detail" v-if="post">
    <header>
      <p class="meta">{{ post.authorName || '管理员' }} · {{ post.publishedAt || '草稿' }}</p>
      <h1>{{ post.title }}</h1>
      <div class="tags">
        <el-tag v-for="tag in post.tagNames" :key="tag" size="small">{{ tag }}</el-tag>
      </div>
    </header>
    <section class="content" v-html="post.content" />
  </article>
  <el-skeleton v-else-if="isLoading" animated :rows="10" />
  <el-empty v-else description="文章不存在或已删除" />
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { usePostStore } from '@/stores/post';

const route = useRoute();
const postStore = usePostStore();

const slug = computed(() => route.params.slug as string);
const post = computed(() => postStore.currentPost);
const isLoading = computed(() => postStore.loading);

const loadPost = () => {
  if (slug.value) {
    postStore.fetchPostDetail(slug.value);
  }
};

onMounted(loadPost);
watch(slug, loadPost);
</script>

<style scoped>
.post-detail {
  background: #fff;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
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
</style>
