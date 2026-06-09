<template>
  <el-row :gutter="18">
    <el-col :xs="24" :lg="10">
      <el-card shadow="never">
        <template #header>
          <span>奖励发放</span>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <el-form-item label="活动ID" prop="activityId">
            <el-input-number v-model="form.activityId" :min="1" class="full-width" />
          </el-form-item>
          <el-form-item label="用户ID" prop="userId">
            <el-input-number v-model="form.userId" :min="1" class="full-width" />
          </el-form-item>
          <el-form-item label="奖励类型" prop="rewardType">
            <el-select v-model="form.rewardType" class="full-width">
              <el-option label="COUPON" value="COUPON" />
              <el-option label="POINT" value="POINT" />
              <el-option label="CASH" value="CASH" />
            </el-select>
          </el-form-item>
          <el-form-item label="奖励金额" prop="rewardAmount">
            <el-input-number v-model="form.rewardAmount" :min="0" :precision="2" class="full-width" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submit">发放奖励</el-button>
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
import { sendReward } from '../api/activity'

const formRef = ref()
const result = ref(null)

const form = reactive({
  activityId: 1,
  userId: 90001,
  rewardType: 'COUPON',
  rewardAmount: 10
})

const rules = {
  activityId: [{ required: true, message: '请输入活动ID', trigger: 'change' }],
  userId: [{ required: true, message: '请输入用户ID', trigger: 'change' }],
  rewardType: [{ required: true, message: '请选择奖励类型', trigger: 'change' }],
  rewardAmount: [{ required: true, message: '请输入奖励金额', trigger: 'change' }]
}

const prettyResult = computed(() => JSON.stringify(result.value, null, 2) || '')

async function submit() {
  await formRef.value.validate()
  result.value = await sendReward(form)
}

function reset() {
  form.activityId = 1
  form.userId = 90001
  form.rewardType = 'COUPON'
  form.rewardAmount = 10
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
