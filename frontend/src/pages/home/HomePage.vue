<template>
  <section class="home-page">
    <div class="layout">
      <div class="main-column">
        <header class="hero">
          <h1>我的博客</h1>
          <p>记录技术、生活与思考，面向开发者的高质量内容平台。</p>
        </header>

        <el-skeleton v-if="isLoading" :rows="4" animated />

        <div v-else>
          <el-empty v-if="!posts.length" description="暂无文章，敬请期待" />
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
              <div class="post-stats">
                <span>{{ post.viewCount ?? 0 }} 阅读</span>
                <span>{{ post.likeCount ?? 0 }} 赞</span>
              </div>
            </article>
          </div>
        </div>
      </div>

      <aside class="sidebar">
        <el-card shadow="never" class="tag-card">
          <template #header>
            <div class="tag-header">
              <span>订阅标签</span>
              <el-button link size="small" @click="loadTags">刷新</el-button>
            </div>
          </template>
          <el-skeleton v-if="tagsLoading" :rows="4" animated />
          <el-empty v-else-if="!tags.length" description="暂无标签" />
          <ul v-else class="tag-list">
            <li v-for="tag in tags" :key="tag.id">
              <div class="tag-info">
                <div class="tag-title">{{ tag.name }}</div>
                <p class="tag-desc">{{ tag.description || '暂无描述' }}</p>
                <span class="tag-count">{{ tag.postCount }} 篇文章</span>
              </div>
              <el-button
                size="small"
                :type="tag.subscribed ? 'success' : 'primary'"
                :plain="tag.subscribed"
                :loading="subscribeLoading === tag.id"
                @click.stop="toggleSubscribe(tag)"
              >
                {{ tag.subscribed ? '已订阅' : '订阅' }}
              </el-button>
            </li>
          </ul>
        </el-card>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { interactionApi } from '@/api/modules/interaction';
import { tagApi } from '@/api/modules/tag';
import { useAuthStore } from '@/stores/auth';
import { usePostStore } from '@/stores/post';
import type { TagItem } from '@/types/tag';

const router = useRouter();
const postStore = usePostStore();
const authStore = useAuthStore();

const tags = ref<TagItem[]>([]);
const tagsLoading = ref(false);
const subscribeLoading = ref<number | null>(null);

const posts = computed(() => postStore.list);
const isLoading = computed(() => postStore.loading);

const goDetail = (slug: string) => {
  router.push({ name: 'PostDetail', params: { slug } });
};

const ensureLogin = () => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录后再订阅');
    return false;
  }
  return true;
};

const loadTags = async () => {
  tagsLoading.value = true;
  try {
    tags.value = await tagApi.fetchAll();
  } catch (error: any) {
    ElMessage.error(error?.message || '标签加载失败');
  } finally {
    tagsLoading.value = false;
  }
};

const toggleSubscribe = async (tag: TagItem) => {
  if (!ensureLogin()) {
    return;
  }
  subscribeLoading.value = tag.id;
  try {
    const result = await interactionApi.toggleTagSubscription(tag.id);
    tag.subscribed = result.subscribed;
    ElMessage.success(result.subscribed ? '订阅成功' : '已取消订阅');
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败');
  } finally {
    subscribeLoading.value = null;
  }
};

onMounted(() => {
  postStore.fetchPosts();
  loadTags();
});
</script>

<style scoped>
.layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.main-column {
  flex: 1;
}

.sidebar {
  width: 320px;
  flex-shrink: 0;
}

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

.post-stats {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
  display: flex;
  gap: 12px;
}

.tag-card {
  width: 100%;
}

.tag-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tag-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.tag-list li:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.tag-info {
  flex: 1;
}

.tag-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.tag-desc {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.tag-count {
  font-size: 12px;
  color: #c0c4cc;
}

@media (max-width: 992px) {
  .layout {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
  }
}
</style>
