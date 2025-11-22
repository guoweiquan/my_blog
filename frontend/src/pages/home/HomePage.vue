<template>
  <section class="home-page">
    <header class="hero">
      <h1>我的博客</h1>
      <p>记录技术、生活与思考，面向开发者的高质量内容平台。</p>
    </header>

    <el-skeleton v-if="isLoading" :rows="4" animated />

    <div v-else class="post-list">
      <article v-for="post in posts" :key="post.id" class="post-card" @click="goDetail(post.slug)">
        <div class="meta">
          <span>{{ post.authorName || '管理员' }}</span>
          <span>·</span>
          <span>{{ post.publishedAt || '草稿' }}</span>
        </div>
        <h2>{{ post.title }}</h2>
        <p>{{ post.summary || '这是一篇精彩的文章，等待完善内容……' }}</p>
        <div class="tags">
          <el-tag v-for="tag in post.tagNames" :key="tag" size="small">{{ tag }}</el-tag>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePostStore } from '@/stores/post';

const router = useRouter();
const postStore = usePostStore();

const posts = computed(() => postStore.list);
const isLoading = computed(() => postStore.loading);

const goDetail = (slug: string) => {
  router.push({ name: 'PostDetail', params: { slug } });
};

onMounted(() => {
  postStore.fetchPosts();
});
</script>

<style scoped>
.hero {
  padding: 48px 0 24px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 16px rgba(31, 45, 61, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(31, 45, 61, 0.12);
}

.meta {
  display: flex;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
