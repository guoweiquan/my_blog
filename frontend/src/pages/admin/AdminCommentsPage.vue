<template>
  <section class="comments-page">
    <div class="toolbar">
      <el-select v-model="filters.status" placeholder="状态" style="width: 160px" @change="handleFilterChange">
        <el-option label="待审核" value="pending" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <div class="spacer" />
      <el-button :loading="loading" @click="loadComments">刷新</el-button>
    </div>

    <el-table :data="comments" border :loading="loading" row-key="id">
      <el-table-column label="用户" width="160">
        <template #default="scope">
          <div class="author">{{ scope.row.authorName }}</div>
          <div class="post">《{{ scope.row.postTitle }}》</div>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="240" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="statusMap[scope.row.status]?.type || 'info'">
            {{ statusMap[scope.row.status]?.label || scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="scope">
          <el-button
            size="small"
            type="success"
            text
            :loading="actionLoading === scope.row.id"
            @click="handleApprove(scope.row)"
          >
            通过
          </el-button>
          <el-button
            size="small"
            type="warning"
            text
            :loading="actionLoading === scope.row.id"
            @click="handleReject(scope.row)"
          >
            驳回
          </el-button>
          <el-button
            size="small"
            type="danger"
            text
            :loading="actionLoading === scope.row.id"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

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
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { adminApi } from '@/api/modules/admin';
import type { CommentModerationItem } from '@/types/comment';

const comments = ref<CommentModerationItem[]>([]);
const loading = ref(false);
const actionLoading = ref<number | null>(null);

const filters = reactive({ status: 'pending' });
const pagination = reactive({ page: 1, size: 10, total: 0 });

const statusMap: Record<string, { label: string; type: string }> = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' }
};

const formatDate = (value?: string) => {
  if (!value) return '-';
  return new Date(value).toLocaleString();
};

const loadComments = async () => {
  loading.value = true;
  try {
    const data = await adminApi.fetchComments({
      status: filters.status,
      page: pagination.page,
      size: pagination.size
    });
    comments.value = data.records;
    pagination.total = data.total;
  } catch (error: any) {
    ElMessage.error(error?.message || '评论加载失败');
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  pagination.page = 1;
  loadComments();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadComments();
};

const runAction = async (id: number, action: () => Promise<void>, successMessage: string) => {
  actionLoading.value = id;
  try {
    await action();
    ElMessage.success(successMessage);
    loadComments();
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败');
  } finally {
    actionLoading.value = null;
  }
};

const handleApprove = (row: CommentModerationItem) => {
  runAction(row.id, () => adminApi.approveComment(row.id), '已通过评论');
};

const handleReject = (row: CommentModerationItem) => {
  runAction(row.id, () => adminApi.rejectComment(row.id), '已驳回评论');
};

const handleDelete = (row: CommentModerationItem) => {
  ElMessageBox.confirm(`确认删除《${row.postTitle}》的该条评论吗？`, '提示', { type: 'warning' })
    .then(() => runAction(row.id, () => adminApi.deleteComment(row.id), '评论已删除'))
    .catch(() => undefined);
};

onMounted(loadComments);
</script>

<style scoped>
.comments-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spacer {
  flex: 1;
}

.author {
  font-weight: 600;
}

.post {
  font-size: 12px;
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
