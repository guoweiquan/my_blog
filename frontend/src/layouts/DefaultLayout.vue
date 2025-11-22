<template>
  <div class="default-layout">
    <AppHeader />
    <main class="page-container">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/common/AppHeader.vue';
import AppFooter from '@/components/common/AppFooter.vue';
</script>

<style scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-container {
  width: min(1200px, 100%);
  margin: 0 auto;
  padding: 24px 16px 48px;
}
</style>
