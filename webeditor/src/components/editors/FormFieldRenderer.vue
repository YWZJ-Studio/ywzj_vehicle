<template>
  <el-form-item
    :label="label"
    :required="required"
    :prop="propName"
  >
    <!-- Namespace ID Select (with x-completion) -->
    <NamespaceIdSelect
      v-if="fieldType === 'string' && !enumValues && hasCompletion"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      :pack-type="completionConfig?.packType"
      :category="completionConfig?.category"
      :field-name="propName"
      :placeholder="translatedDescription"
    />

    <!-- String Input (fallback) -->
    <el-input
      v-else-if="fieldType === 'string' && !enumValues"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      :placeholder="translatedDescription"
      clearable
    />

    <!-- Enum Select -->
    <el-select
      v-else-if="fieldType === 'string' && enumValues"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      :placeholder="translatedDescription"
      clearable
      style="width: 100%"
    >
      <el-option
        v-for="option in enumValues"
        :key="option"
        :label="option"
        :value="option"
      />
    </el-select>

    <!-- Number Input -->
    <el-input-number
      v-else-if="fieldType === 'number'"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      :min="propSchema.minimum"
      :max="propSchema.maximum"
      :step="getNumberStep()"
      :placeholder="translatedDescription"
      style="width: 100%"
    />

    <!-- Boolean Switch -->
    <el-switch
      v-else-if="fieldType === 'boolean'"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
    />

    <!-- Array of Strings -->
    <div v-else-if="fieldType === 'array' && arrayItemType === 'string'" class="array-field">
      <el-tag
        v-for="(item, idx) in (modelValue || [])"
        :key="idx"
        closable
        @close="removeArrayItem(idx as number)"
        style="margin-right: 8px; margin-bottom: 8px"
      >
        {{ item }}
      </el-tag>
      <el-input
        v-model="newArrayItem"
        placeholder="输入后按回车添加"
        size="small"
        style="width: 200px"
        @keyup.enter="addArrayItem"
      >
        <template #append>
          <el-button :icon="Plus" @click="addArrayItem" />
        </template>
      </el-input>
    </div>

    <!-- Array of Numbers (Vec3) -->
    <div v-else-if="fieldType === 'array' && arrayItemType === 'number'" class="vec3-field">
      <el-input-number
        v-for="(val, idx) in getArrayValue()"
        :key="idx"
        :model-value="val"
        @update:model-value="updateArrayItem(idx as number, $event)"
        :placeholder="`${['X', 'Y', 'Z'][idx as number] || idx}`"
        style="width: 100px; margin-right: 8px"
      />
    </div>

    <!-- Object Field -->
    <div v-else-if="fieldType === 'object'" class="object-field">
      <el-card shadow="never" :body-style="{ padding: '12px' }">
        <FormFieldRenderer
          v-for="(subPropSchema, subPropName) in propSchema.properties"
          :key="String(subPropName)"
          :prop-name="String(subPropName)"
          :prop-schema="subPropSchema"
          :root-schema="rootSchema"
          :model-value="(modelValue || {})[subPropName]"
          :required="propSchema.required?.includes(String(subPropName))"
          @update:modelValue="updateObjectField(String(subPropName), $event)"
        />
      </el-card>
    </div>

    <!-- Array of Objects -->
    <div v-else-if="fieldType === 'array' && arrayItemType === 'object'" class="array-object-field">
      <div
        v-for="(item, idx) in (modelValue || [])"
        :key="idx"
        class="array-object-item"
      >
        <el-card shadow="never">
          <template #header>
            <div class="array-object-header">
              <span>{{ propName }} #{{ (idx as number) + 1 }}</span>
              <el-button
                :icon="Delete"
                size="small"
                text
                type="danger"
                @click="removeArrayItem(idx as number)"
              />
            </div>
          </template>
          <FormFieldRenderer
            v-for="(subPropSchema, subPropName) in getArrayItemSchema().properties"
            :key="String(subPropName)"
            :prop-name="String(subPropName)"
            :prop-schema="subPropSchema"
            :root-schema="rootSchema"
            :model-value="item[subPropName]"
            :required="getArrayItemSchema().required?.includes(String(subPropName))"
            @update:modelValue="updateArrayObjectItem(idx as number, String(subPropName), $event)"
          />
        </el-card>
      </div>
      <el-button :icon="Plus" @click="addArrayObjectItem" style="width: 100%">
        添加{{ label }}
      </el-button>
    </div>

    <!-- Fallback: JSON String -->
    <el-input
      v-else
      :model-value="typeof modelValue === 'string' ? modelValue : JSON.stringify(modelValue)"
      @update:model-value="emit('update:modelValue', $event)"
      type="textarea"
      :rows="3"
      :placeholder="translatedDescription"
    />

    <template #label>
      <span class="form-label">
        {{ label }}
        <el-tooltip v-if="translatedDescription" :content="translatedDescription" placement="top">
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </span>
    </template>
  </el-form-item>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {Delete, Plus, QuestionFilled} from '@element-plus/icons-vue';
import {resolveRef} from '@/utils/schemaUtils';
import NamespaceIdSelect from './NamespaceIdSelect.vue';

const {t} = useI18n();

interface Props {
  propName: string;
  propSchema: any;
  rootSchema?: any;
  modelValue: any;
  required?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: any];
}>();

const newArrayItem = ref('');

const label = computed(() => {
  // 转换驼峰命名为可读标签
  return props.propName
    .replace(/_/g, ' ')
    .replace(/\b\w/g, c => c.toUpperCase());
});

const fieldType = computed(() => {
  if (props.propSchema.type) {
    return props.propSchema.type;
  }
  return 'string';
});

const enumValues = computed(() => {
  return props.propSchema.enum || null;
});

// 检查是否有 x-completion 配置
const hasCompletion = computed(() => {
  return !!props.propSchema['x-completion'];
});

// 获取 x-completion 配置
const completionConfig = computed(() => {
  return props.propSchema['x-completion'] || null;
});

const translatedDescription = computed(() => {
  const desc = props.propSchema.description;
  if (!desc) return '';
  // 如果是翻译键（如 vehicle.type），则翻译；否则直接返回
  return desc.includes('.') ? t(desc) : desc;
});

const arrayItemType = computed(() => {
  if (props.propSchema.items?.type) {
    return props.propSchema.items.type;
  }
  if (props.propSchema.items?.$ref) {
    return 'object';
  }
  return 'string';
});

function getNumberStep() {
  const min = props.propSchema.minimum || 0;
  const max = props.propSchema.maximum;

  // 如果是小数范围，使用小步长
  if (min < 1 || (max && max < 10)) {
    return 0.01;
  }
  return 1;
}

function getArrayValue() {
  if (!props.modelValue) {
    const minItems = props.propSchema.minItems || 3;
    return Array(minItems).fill(0);
  }
  return props.modelValue;
}

function updateArrayItem(index: number, value: any) {
  const arr = [...getArrayValue()];
  arr[index] = value;
  emit('update:modelValue', arr);
}

function addArrayItem() {
  if (!newArrayItem.value.trim()) return;

  const arr = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
  arr.push(newArrayItem.value.trim());
  emit('update:modelValue', arr);
  newArrayItem.value = '';
}

function removeArrayItem(index: number) {
  const arr = [...(props.modelValue || [])];
  arr.splice(index, 1);
  emit('update:modelValue', arr);
}

function updateObjectField(fieldName: string, value: any) {
  const obj = { ...(props.modelValue || {}) };
  obj[fieldName] = value;
  emit('update:modelValue', obj);
}

function getArrayItemSchema() {
  // 处理 $ref 引用
  if (props.propSchema.items?.$ref && props.rootSchema) {
    return resolveRef(props.propSchema.items.$ref, props.rootSchema);
  }
  return props.propSchema.items || {};
}

function updateArrayObjectItem(arrayIndex: number, fieldName: string, value: any) {
  const arr = [...(props.modelValue || [])];
  arr[arrayIndex] = {
    ...arr[arrayIndex],
    [fieldName]: value
  };
  emit('update:modelValue', arr);
}

function addArrayObjectItem() {
  const arr = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
  const newItem: any = {};

  // 使用默认值初始化新项
  const itemSchema = getArrayItemSchema();
  if (itemSchema.properties) {
    Object.entries(itemSchema.properties).forEach(([key, schema]: [string, any]) => {
      if (schema.default !== undefined) {
        newItem[key] = schema.default;
      }
    });
  }

  arr.push(newItem);
  emit('update:modelValue', arr);
}
</script>

<style scoped>
.form-label {
  display: flex;
  align-items: center;
  gap: 4px;
}

.help-icon {
  color: var(--el-color-info);
  cursor: help;
}

.array-field {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.vec3-field {
  display: flex;
  gap: 8px;
}

.object-field {
  width: 100%;
}

.array-object-field {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.array-object-item {
  width: 100%;
}

.array-object-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
