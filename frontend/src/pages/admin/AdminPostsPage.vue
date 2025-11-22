<template>
  <section>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索文章" clearable style="width: 240px" />
      <div class="spacer" />
      <el-button type="primary">新建文章</el-button>
    </div>
    <el-table :data="filteredPosts" border>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="publishedAt" label="发布时间" width="180" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button size="small" text>编辑</el-button>
          <el-button size="small" text type="danger">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

const keyword = ref('');
const posts = ref([
  { id: 1, title: '示例文章 A', status: 'published', publishedAt: '2024-01-01' },
  { id: 2, title: '示例文章 B', status: 'draft', publishedAt: '-' }
]);

const filteredPosts = computed(() => {
  if (!keyword.value) return posts.value;
  return posts.value.filter((post) => post.title.includes(keyword.value));
});
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.spacer {
  flex: 1;
}
</style>
