<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" :loading="loading">
          <p class="label">{{ card.label }}</p>
          <p class="value">{{ card.value }}</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="hot-card" shadow="never">
      <template #header>
        <div class="hot-header">
          <h3>热门文章排行</h3>
          <el-button link size="small" @click="loadOverview" :loading="loading">刷新</el-button>
        </div>
      </template>
      <el-table
        :data="hotPosts"
        border
        empty-text="暂无数据"
        v-loading="loading"
        row-key="postId"
      >
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="scope">
            <RouterLink :to="{ name: 'PostDetail', params: { slug: scope.row.slug } }" target="_blank">
              {{ scope.row.title }}
            </RouterLink>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="120" />
        <el-table-column prop="score" label="热度分" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { analyticsApi } from '@/api/modules/analytics';
import type { AnalyticsOverview } from '@/types/analytics';

const overview = ref<AnalyticsOverview | null>(null);
const loading = ref(false);

const cards = computed(() => {
  const data = overview.value;
  return [
    { label: '今日 PV', value: data ? data.todayPv : '--' },
    { label: '今日 UV', value: data ? data.todayUv : '--' },
    { label: '文章总数', value: data ? data.publishedPosts : '--' },
    { label: '待审核评论', value: data ? data.pendingComments : '--' }
  ];
});

const hotPosts = computed(() => overview.value?.hotPosts ?? []);

const loadOverview = async () => {
  loading.value = true;
  try {
    overview.value = await analyticsApi.fetchOverview();
  } catch (error: any) {
    ElMessage.error(error?.message || '仪表盘数据加载失败');
  } finally {
    loading.value = false;
  }
};

onMounted(loadOverview);
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.label {
  color: #909399;
  margin: 0 0 8px;
}

.value {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.hot-card {
  min-height: 240px;
}

.hot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
