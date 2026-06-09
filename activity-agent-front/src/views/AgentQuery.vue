<template>
  <el-card shadow="never">
    <template #header>
      <div class="header-row">
        <span>自然语言查询</span>
        <el-space wrap>
          <el-tag
            v-for="item in examples"
            :key="item"
            class="example-tag"
            @click="question = item"
          >
            {{ item }}
          </el-tag>
        </el-space>
      </div>
    </template>

    <el-input
      v-model="question"
      type="textarea"
      :rows="4"
      placeholder="请输入自然语言问题，例如：统计最近7天各活动的参与人数"
    />

    <div class="actions">
      <el-button type="primary" :loading="loading" @click="handleQuery">开始查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <template v-if="result">
      <el-row :gutter="18" class="result-area">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <span>生成 SQL</span>
            </template>
            <pre class="code-block">{{ result.generatedSql }}</pre>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="answer-card">
            <template #header>
              <span>分析结论</span>
            </template>
            <div class="answer-text">{{ result.answer }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="18" class="result-area">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>
              <span>查询结果</span>
            </template>
            <el-table :data="result.queryResult || []" stripe>
              <el-table-column
                v-for="column in tableColumns"
                :key="column"
                :prop="column"
                :label="column"
                min-width="140"
              />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </el-card>
</template>

<script setup>
import { computed, ref } from 'vue'
import { queryAgent } from '../api/agent'

const loading = ref(false)
const question = ref('统计最近7天各活动的参与人数')
const result = ref(null)

const examples = [
  '统计最近7天各活动的参与人数',
  '查询618活动奖励发放成功率',
  '对比 APP 和 H5 渠道参与人数',
  '查询奖励发放失败最多的活动'
]

const tableColumns = computed(() => {
  const firstRow = result.value?.queryResult?.[0]
  return firstRow ? Object.keys(firstRow) : []
})

async function handleQuery() {
  if (!question.value.trim()) return
  loading.value = true
  try {
    const res = await queryAgent({
      question: question.value,
      user_id: 1
    })
    result.value = res.data
    const current = Number(localStorage.getItem('agentQueryCount') || 0)
    localStorage.setItem('agentQueryCount', String(current + 1))
  } finally {
    loading.value = false
  }
}

function reset() {
  question.value = ''
  result.value = null
}
</script>

<style scoped>
.header-row {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.example-tag {
  cursor: pointer;
}

.actions {
  margin-top: 16px;
}

.result-area {
  margin-top: 18px;
}

.code-block {
  min-height: 240px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 12px;
  background: #0f1b31;
  color: #dce7ff;
}

.answer-card {
  height: 100%;
}

.answer-text {
  min-height: 240px;
  padding: 16px;
  line-height: 1.8;
  color: #233552;
  border-radius: 12px;
  background: linear-gradient(180deg, #f9fbff 0%, #f2f6fd 100%);
}
</style>
