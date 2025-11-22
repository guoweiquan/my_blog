<template>
  <aside class="admin-sidebar">
    <el-menu :default-active="active" router>
      <el-menu-item index="/admin/dashboard">仪表盘</el-menu-item>
      <el-menu-item index="/admin/posts">文章管理</el-menu-item>
      <el-menu-item index="/admin/comments">评论审核</el-menu-item>
      <el-menu-item index="/admin/tags">标签管理</el-menu-item>
      <el-menu-item index="/admin/settings">站点设置</el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const active = computed(() => `/admin/${route.path.split('/')[2] || 'dashboard'}`);
</script>

<style scoped>
.admin-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #ebeef5;
}

:deep(.el-menu) {
  border-right: none;
  min-height: calc(100vh - 60px);
}
</style>
