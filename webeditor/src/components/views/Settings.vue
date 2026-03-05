<template>
  <div class="settings-view">
    <div class="settings-header">
      <h2>设置</h2>
    </div>

    <div class="settings-content">
      <div class="setting-item">
        <div class="setting-label">语言</div>
        <el-select v-model="currentLocale" placeholder="选择语言" @change="handleLocaleChange">
          <el-option label="简体中文" value="zh-CN" />
          <el-option label="English" value="en-US" />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

const { locale } = useI18n();
const currentLocale = ref(locale.value);

function handleLocaleChange(value: string) {
  locale.value = value;
  localStorage.setItem('locale', value);
}
</script>

<style scoped>
.settings-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.settings-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--el-border-color);
}

.settings-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.settings-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.setting-label {
  min-width: 80px;
  font-weight: 500;
}
</style>
