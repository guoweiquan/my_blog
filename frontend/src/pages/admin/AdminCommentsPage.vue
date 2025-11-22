<template>
  <section>
    <el-alert title="当前评论审核流程为人工确认" type="info" show-icon class="mb-16" />
    <el-table :data="comments" border>
      <el-table-column prop="author" label="用户" width="140" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" type="success" text>通过</el-button>
          <el-button size="small" type="warning" text>驳回</el-button>
          <el-button size="small" type="danger" text>删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const comments = ref([
  { id: 1, author: '访客A', content: '期待更多文章！', status: 'pending' },
  { id: 2, author: '用户B', content: '写得很详细。', status: 'approved' }
]);
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>
