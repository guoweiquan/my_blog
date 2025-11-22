<template>
  <header class="app-header">
    <RouterLink class="logo" to="/">MyBlog</RouterLink>
    <nav class="nav">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/admin/dashboard" v-if="authStore.isAdmin">后台</RouterLink>
    </nav>
    <div class="actions">
      <el-button v-if="!authStore.isAuthenticated" size="small" @click="goLogin">登录</el-button>
      <el-dropdown v-else>
        <span class="el-dropdown-link">
          {{ authStore.displayName }}
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="goAdmin" v-if="authStore.isAdmin">后台管理</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const goLogin = () => router.push({ name: 'Login' });
const goAdmin = () => router.push({ name: 'AdminDashboard' });
const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'Home' });
};
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.logo {
  font-weight: 600;
  font-size: 20px;
  color: #1f2d3d;
  text-decoration: none;
  margin-right: 32px;
}

.nav {
  display: flex;
  gap: 16px;
}

.nav a {
  text-decoration: none;
  color: #606266;
}

.nav a.router-link-active {
  color: #409eff;
}

.actions {
  margin-left: auto;
}
</style>
