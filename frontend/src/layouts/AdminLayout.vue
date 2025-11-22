<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="brand">博客后台</div>
      <div class="spacer" />
      <span class="user">{{ authStore.displayName }}</span>
      <el-button type="primary" size="small" @click="handleLogout">退出</el-button>
    </header>
    <div class="admin-body">
      <AdminSidebar />
      <section class="admin-content">
        <RouterView />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import AdminSidebar from '@/components/common/AdminSidebar.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'Login' });
};
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.admin-header {
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 60px;
  background: #1f2d3d;
  color: #fff;
}

.brand {
  font-weight: 600;
  letter-spacing: 1px;
}

.spacer {
  flex: 1;
}

.admin-body {
  flex: 1;
  display: flex;
  background: #f5f7fa;
}

.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
