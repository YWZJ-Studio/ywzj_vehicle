<template>
  <div class="json-form-editor">
    <el-scrollbar class="form-scrollbar">
      <el-form
        ref="formRef"
        :model="formData"
        label-width="160px"
        label-position="left"
        class="form-content"
        @submit.prevent
      >
        <div v-if="schemaInfo">
          <div class="form-header">
            <h3>{{ schemaInfo.title }}</h3>
            <el-tag v-if="validationResult && !validationResult.valid" type="danger" size="small">
              {{ validationResult.errors?.length || 0 }} 个错误
            </el-tag>
            <el-tag v-else type="success" size="small">验证通过</el-tag>
          </div>

          <el-alert
            v-if="validationResult && !validationResult.valid"
            type="error"
            :closable="false"
            class="validation-alert"
          >
            <template #title>验证错误</template>
            <ul class="error-list">
              <li v-for="(err, idx) in validationResult.errors" :key="idx">
                <strong>{{ err.field }}:</strong> {{ err.message }}
              </li>
            </ul>
          </el-alert>

          <FormFieldRenderer
            v-for="(propSchema, propName) in schemaInfo.schema.properties"
            :key="String(propName)"
            :prop-name="String(propName)"
            :prop-schema="propSchema"
            :root-schema="schemaInfo.schema"
            :model-value="formData[String(propName)]"
            :required="schemaInfo.schema.required?.includes(String(propName))"
            @update:modelValue="updateField(String(propName), $event)"
          />
        </div>

        <el-empty v-else description="无法识别的文件类型，无法生成表单" />
      </el-form>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref, watch} from 'vue';
import {ElMessage} from 'element-plus';
import {findSchemaByPath, type SchemaInfo} from '@/utils/schemaRegistry';
import {validateByPath, type ValidationResult} from '@/utils/validator';
import {parseJsonWithComments} from '@/utils/jsonParser';
import FormFieldRenderer from './FormFieldRenderer.vue';

interface Props {
  content: string;
  path: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  update: [content: string];
}>();

const formData = ref<Record<string, any>>({});
const schemaInfo = ref<SchemaInfo | null>(null);
const validationResult = ref<ValidationResult | null>(null);

// 加载 Schema 和初始数据
onMounted(() => {
  loadSchema();
  parseContent();
});

// 监听内容变化
watch(() => props.content, () => {
  parseContent();
});

// 监听路径变化
watch(() => props.path, () => {
  loadSchema();
  parseContent();
});

function loadSchema() {
  schemaInfo.value = findSchemaByPath(props.path);
}

function parseContent() {
  try {
    const parsed = parseJsonWithComments(props.content);
    formData.value = { ...parsed };
    validateForm();
  } catch (err) {
    console.error('Failed to parse JSON:', err);
    ElMessage.error('JSON 解析失败');
  }
}

function updateField(fieldName: string, value: any) {
  formData.value[fieldName] = value;
  emitUpdate();
}

function emitUpdate() {
  try {
    const jsonString = JSON.stringify(formData.value, null, 2);
    emit('update', jsonString);
    validateForm();
  } catch (err) {
    console.error('Failed to stringify JSON:', err);
  }
}

function validateForm() {
  validationResult.value = validateByPath(props.path, formData.value);
}
</script>

<style scoped>
.json-form-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.form-scrollbar {
  flex: 1;
  height: 100%;
}

.form-content {
  padding: 20px;
  max-width: 800px;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color);
}

.form-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  flex: 1;
}

.validation-alert {
  margin-bottom: 20px;
}

.error-list {
  margin: 8px 0 0 0;
  padding-left: 20px;
}

.error-list li {
  margin: 4px 0;
  font-size: 13px;
}
</style>
