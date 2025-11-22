<template>
  <section>
    <div class="toolbar">
      <el-select v-model="status" placeholder="状态" clearable style="width: 160px">
        <el-option label="全部" value="" />
        <el-option label="已发布" value="published" />
        <el-option label="草稿" value="draft" />
      </el-select>
      <el-button :loading="loading" @click="loadData">查询</el-button>
      <div class="spacer" />
      <el-button type="primary" @click="openDialog()">新建文章</el-button>
    </div>
    <el-table :data="posts" border :loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'published' ? 'success' : 'info'">
            {{ scope.row.status === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" width="200" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button size="small" text @click="openDialog(scope.row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑文章' : '新建文章'" width="720px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="dialogFormRef">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="Slug" prop="slug">
          <el-input v-model="form.slug" placeholder="URL 别名，如 my-post" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" rows="6" placeholder="支持 Markdown" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPost">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { postApi } from '@/api/modules/post';
import type { PostSummary } from '@/types/post';

const posts = ref<PostSummary[]>([]);
const loading = ref(false);
const saving = ref(false);
const status = ref<string | ''>('');
const pagination = reactive({ page: 1, size: 10, total: 0 });
const dialogVisible = ref(false);
const dialogFormRef = ref<FormInstance>();

const form = reactive({
  id: 0,
  title: '',
  slug: '',
  status: 'draft',
  summary: '',
  content: ''
});

const rules: FormRules<typeof form> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  slug: [{ required: true, message: '请输入 Slug', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
};

const fetchParams = computed(() => ({
  page: pagination.page,
  size: pagination.size,
  status: status.value || undefined
}));

const loadData = async () => {
  loading.value = true;
  try {
    const data = await postApi.fetchManagePosts(fetchParams.value);
    posts.value = data.records;
    pagination.total = data.total;
  } catch (error: any) {
    ElMessage.error(error?.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadData();
};

const resetForm = () => {
  form.id = 0;
  form.title = '';
  form.slug = '';
  form.status = 'draft';
  form.summary = '';
  form.content = '';
};

const openDialog = async (row?: PostSummary) => {
  resetForm();
  if (row) {
    form.id = row.id;
    const detail = await postApi.fetchPostDetail(row.slug);
    form.title = detail.title;
    form.slug = detail.slug;
    form.status = detail.status;
    form.summary = detail.summary || '';
    form.content = detail.content;
  }
  dialogVisible.value = true;
};

const submitPost = () => {
  dialogFormRef.value?.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    const payload = {
      title: form.title,
      slug: form.slug,
      summary: form.summary,
      content: form.content,
      status: form.status
    };
    try {
      if (form.id) {
        await postApi.updatePost(form.id, payload);
        ElMessage.success('文章已更新');
      } else {
        await postApi.createPost(payload);
        ElMessage.success('文章已创建');
      }
      dialogVisible.value = false;
      loadData();
    } catch (error: any) {
      ElMessage.error(error?.message || '操作失败');
    } finally {
      saving.value = false;
    }
  });
};

const handleDelete = (row: PostSummary) => {
  ElMessageBox.confirm(`确定删除《${row.title}》吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await postApi.deletePost(row.id);
      ElMessage.success('已删除');
      loadData();
    })
    .catch(() => undefined);
};

onMounted(() => {
  loadData();
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

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
