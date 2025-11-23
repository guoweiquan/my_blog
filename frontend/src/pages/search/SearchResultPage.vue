<template>
  <section class="search-page">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="输入关键字"
        size="large"
        @keyup.enter="handleSearch"
        clearable
      >
        <template #suffix>
          <el-icon class="icon-button" @click="handleSearch"><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-alert
      v-if="hasSearched"
      type="info"
      show-icon
      :title="`找到 ${pagination.total} 条相关结果`"
      class="mb-16"
    />

    <el-skeleton v-if="loading" :rows="4" animated />

    <el-empty v-else-if="!results.length"
              description="暂未找到相关内容，换个关键词试试吧" />

    <div v-else class="result-list">
      <article v-for="item in results" :key="item.id" class="result-card" @click="goDetail(item.slug)">
        <h3>{{ item.title }}</h3>
        <p>{{ item.summary || '这篇文章暂无摘要' }}</p>
        <div class="meta">
          <span>{{ item.authorName || '管理员' }}</span>
          <span>·</span>
          <span>{{ item.publishedAt || '草稿' }}</span>
          <span>·</span>
          <span>{{ item.viewCount ?? 0 }} 阅读</span>
        </div>
        <div class="tags" v-if="item.tagNames?.length">
          <el-tag v-for="tag in item.tagNames" :key="tag" size="small">{{ tag }}</el-tag>
        </div>
      </article>
      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="pagination.total"
          :page-size="pagination.size"
          :current-page="pagination.page"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { postApi } from '@/api/modules/post';
import type { PostSummary } from '@/types/post';

const router = useRouter();
const route = useRoute();
const keyword = ref<string>((route.query.q as string) || '');
const results = ref<PostSummary[]>([]);
const loading = ref(false);
const hasSearched = ref(false);
const pagination = reactive({ page: 1, size: 10, total: 0 });

const fetchResults = async () => {
  hasSearched.value = true;
  if (!keyword.value.trim()) {
    results.value = [];
    pagination.total = 0;
    return;
  }
  loading.value = true;
  try {
    const data = await postApi.searchPosts({ q: keyword.value.trim(), page: pagination.page, size: pagination.size });
    results.value = data.records;
    pagination.total = data.total;
    pagination.size = data.size;
  } catch (error: any) {
    ElMessage.error(error?.message || '搜索失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  const query = keyword.value.trim();
  pagination.page = 1;
  const navigation = query
    ? router.push({ name: 'SearchResult', query: { q: query } })
    : router.push({ name: 'SearchResult' });
  navigation.catch(() => undefined);
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  fetchResults();
};

const goDetail = (slug: string) => {
  router.push({ name: 'PostDetail', params: { slug } });
};

watch(
  () => route.query.q,
  (q) => {
    keyword.value = typeof q === 'string' ? q : '';
    pagination.page = 1;
    fetchResults();
  },
  { immediate: true }
);
</script>

<style scoped>
.search-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  display: flex;
  gap: 12px;
}

.icon-button {
  cursor: pointer;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 16px rgba(31, 45, 61, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.result-card:hover {
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
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.mb-16 {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
  }
}
</style>
