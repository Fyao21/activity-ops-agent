<template>
  <el-row :gutter="18">
    <el-col :xs="24" :lg="10">
      <el-card shadow="never">
        <template #header>
          <span>用户参与活动</span>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <el-form-item label="活动ID" prop="activityId">
            <el-input-number v-model="form.activityId" :min="1" class="full-width" />
          </el-form-item>
          <el-form-item label="用户ID" prop="userId">
            <el-input-number v-model="form.userId" :min="1" class="full-width" />
          </el-form-item>
          <el-form-item label="参与渠道" prop="channel">
            <el-select v-model="form.channel" class="full-width">
              <el-option label="APP" value="APP" />
              <el-option label="H5" value="H5" />
              <el-option label="WEB" value="WEB" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submit">提交参与</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :xs="24" :lg="14">
      <el-card shadow="never">
        <template #header>
          <span>接口返回结果</span>
        </template>
        <pre class="result-block">{{ prettyResult }}</pre>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { participateActivity } from '../api/activity'

const formRef = ref()
const result = ref(null)

const form = reactive({
  activityId: 1,
  userId: 90001,
  channel: 'APP'
})

const rules = {
  activityId: [{ required: true, message: '请输入活动ID', trigger: 'change' }],
  userId: [{ required: true, message: '请输入用户ID', trigger: 'change' }],
  channel: [{ required: true, message: '请选择渠道', trigger: 'change' }]
}

const prettyResult = computed(() => JSON.stringify(result.value, null, 2) || '')

async function submit() {
  await formRef.value.validate()
  result.value = await participateActivity(form)
}

function reset() {
  form.activityId = 1
  form.userId = 90001
  form.channel = 'APP'
  result.value = null
}
</script>

<style scoped>
.full-width {
  width: 100%;
}

.result-block {
  min-height: 280px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 12px;
  background: #0f1b31;
  color: #dce7ff;
}
</style>
