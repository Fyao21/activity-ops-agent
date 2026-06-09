<template>
  <el-card shadow="never">
    <template #header>
      <span>活动统计查询</span>
    </template>

    <el-form :inline="true" :model="queryForm" class="query-form">
      <el-form-item label="活动ID">
        <el-input-number v-model="queryForm.activityId" :min="1" />
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker
          v-model="queryForm.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
        />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker
          v-model="queryForm.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" stripe>
      <el-table-column prop="activityId" label="活动ID" width="100" />
      <el-table-column prop="statDate" label="统计日期" width="140" />
      <el-table-column prop="participantCount" label="参与人数" width="120" />
      <el-table-column prop="rewardCount" label="奖励数量" width="120" />
      <el-table-column prop="rewardSuccessCount" label="奖励成功数" width="140" />
      <el-table-column prop="conversionRate" label="转化率" width="120" />
      <el-table-column prop="retentionRate" label="留存率" width="120" />
      <el-table-column prop="updateTime" label="更新时间" min-width="180" />
    </el-table>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { getStatistics } from '../api/activity'

const queryForm = reactive({
  activityId: 1,
  startDate: '',
  endDate: ''
})

const tableData = ref([])

async function loadData() {
  const params = {}
  if (queryForm.activityId) params.activityId = queryForm.activityId
  if (queryForm.startDate) params.startDate = queryForm.startDate
  if (queryForm.endDate) params.endDate = queryForm.endDate
  const res = await getStatistics(params)
  tableData.value = res.data || []
}

function resetQuery() {
  queryForm.activityId = 1
  queryForm.startDate = ''
  queryForm.endDate = ''
  tableData.value = []
}
</script>

<style scoped>
.query-form {
  margin-bottom: 18px;
}
</style>
