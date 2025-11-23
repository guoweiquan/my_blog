<template>
  <li class="comment-item">
    <div class="comment-header">
      <div class="author">{{ comment.authorName }}</div>
      <span class="date">{{ formattedDate }}</span>
    </div>
    <p class="comment-content">{{ comment.content }}</p>
    <div class="comment-actions">
      <el-button class="reply-btn" text type="primary" size="small" @click="handleReply">回复</el-button>
    </div>
    <ul v-if="comment.children?.length" class="comment-children">
      <CommentItemNode
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        @reply="emit('reply', $event)"
      />
    </ul>
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { CommentItem } from '@/types/comment';

defineOptions({ name: 'CommentItemNode' });

const props = defineProps<{ comment: CommentItem }>();
const emit = defineEmits<{ (e: 'reply', comment: CommentItem): void }>();

const formattedDate = computed(() => {
  if (!props.comment.createdAt) return '';
  return new Date(props.comment.createdAt).toLocaleString();
});

const handleReply = () => {
  emit('reply', props.comment);
};
</script>

<style scoped>
.comment-item {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  color: #909399;
}

.author {
  font-weight: 600;
  color: #303133;
}

.comment-content {
  margin: 4px 0 8px;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #333;
}

.comment-actions {
  text-align: right;
}

.reply-btn {
  padding: 0;
}

.comment-children {
  list-style: none;
  margin: 12px 0 0 16px;
  padding-left: 12px;
  border-left: 2px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
