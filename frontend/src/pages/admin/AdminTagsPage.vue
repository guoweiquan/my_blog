<template>
  <section>
    <div class="toolbar">
      <el-input v-model="newTag" placeholder="新标签名称" style="width: 220px" />
      <el-button type="primary" @click="handleCreate">添加标签</el-button>
    </div>
    <el-table :data="tags" border>
      <el-table-column prop="name" label="标签名" />
      <el-table-column prop="postCount" label="文章数" width="120" />
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
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const newTag = ref('');
const tags = ref([
  { id: 1, name: 'Vue', postCount: 3 },
  { id: 2, name: 'Spring Boot', postCount: 5 }
]);

const handleCreate = () => {
  if (!newTag.value.trim()) {
    return ElMessage.warning('请输入标签名');
  }
  tags.value.push({ id: Date.now(), name: newTag.value, postCount: 0 });
  newTag.value = '';
  ElMessage.success('标签已创建（示例数据）');
};
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
