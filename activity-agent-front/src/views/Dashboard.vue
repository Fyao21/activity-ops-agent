<template>
  <div>
    <el-row :gutter="18">
      <el-col v-for="card in cards" :key="card.label" :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-tip">{{ card.tip }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18" class="block-gap">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>
            <div class="section-header">
              <span>最近活动列表</span>
              <el-button text @click="loadData">刷新</el-button>
            </div>
          </template>

          <el-table :data="recentActivities" stripe>
            <el-table-column prop="id" label="活动ID" width="90" />
            <el-table-column prop="activityName" label="活动名称" min-width="180" />
            <el-table-column prop="activityType" label="活动类型" width="140" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" min-width="180" />
            <el-table-column prop="endTime" label="结束时间" min-width="180" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="tip-card">
          <template #header>
            <span>演示建议</span>
          </template>
          <div class="tip-item">1. 先在活动管理页面创建一个新的活动。</div>
          <div class="tip-item">2. 再到用户参与页面提交参与请求。</div>
          <div class="tip-item">3. 然后到奖励发放页面发奖。</div>
          <div class="tip-item">4. 最后在 Agent 查询页面输入自然语言问题，展示完整链路。</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getActivityList, getStatistics } from '../api/activity'

const activities = ref([])
const statistics = ref([])
const agentQueryCount = ref(Number(localStorage.getItem('agentQueryCount') || 0))

const cards = computed(() => {
  const participantCount = statistics.value.reduce((sum, item) => sum + (item.participantCount || 0), 0)
  const rewardCount = statistics.value.reduce((sum, item) => sum + (item.rewardCount || 0), 0)

  return [
    { label: '活动总数', value: activities.value.length, tip: '当前已加载的活动数量' },
    { label: '参与人数', value: participantCount, tip: '按统计表汇总' },
    { label: '奖励发放数量', value: rewardCount, tip: '按统计表汇总' },
    { label: 'Agent 查询次数', value: agentQueryCount.value, tip: '浏览器本地演示计数' }
  ]
})

const recentActivities = computed(() => activities.value.slice(0, 5))

async function loadData() {
  const [activityRes, statisticsRes] = await Promise.all([
    getActivityList({ page: 1, pageSize: 100 }),
    getStatistics({})
  ])
  activities.value = activityRes.data || []
  statistics.value = statisticsRes.data || []
  agentQueryCount.value = Number(localStorage.getItem('agentQueryCount') || 0)
}

function statusText(status) {
  if (status === 1) return '进行中'
  if (status === 2) return '已结束'
  return '未开始'
}

function statusType(status) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

onMounted(loadData)
</script>

<style scoped>
.block-gap {
  margin-top: 18px;
}

.stat-card {
  border: none;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.stat-label {
  color: #6e7b90;
  font-size: 14px;
}

.stat-value {
  margin-top: 14px;
  font-size: 34px;
  font-weight: 700;
  color: #16233b;
}

.stat-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #8a97ab;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.tip-card {
  height: 100%;
}

.tip-item {
  padding: 12px 0;
  line-height: 1.7;
  color: #41516f;
  border-bottom: 1px dashed #e6ebf5;
}

.tip-item:last-child {
  border-bottom: none;
}
</style>
