<template>
  <div>
    <el-row :gutter="18">
      <el-col :xs="24" :lg="9">
        <el-card shadow="never">
          <template #header>
            <span>创建活动</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
            <el-form-item label="活动名称" prop="activityName">
              <el-input v-model="form.activityName" placeholder="请输入活动名称" />
            </el-form-item>
            <el-form-item label="活动类型" prop="activityType">
              <el-input v-model="form.activityType" placeholder="例如 NEW_USER" />
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择开始时间"
                class="full-width"
              />
            </el-form-item>
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择结束时间"
                class="full-width"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" class="full-width">
                <el-option label="未开始" :value="0" />
                <el-option label="进行中" :value="1" />
                <el-option label="已结束" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="规则描述" prop="ruleDesc">
              <el-input v-model="form.ruleDesc" type="textarea" :rows="4" placeholder="请输入活动规则" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitForm">创建活动</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="15">
        <el-card shadow="never">
          <template #header>
            <div class="table-header">
              <span>活动列表</span>
              <el-button text @click="loadList">刷新</el-button>
            </div>
          </template>

          <el-table :data="tableData" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="activityName" label="活动名称" min-width="180" />
            <el-table-column prop="activityType" label="类型" width="130" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" min-width="160" />
            <el-table-column prop="endTime" label="结束时间" min-width="160" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button text type="primary" @click="showDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="page"
              :page-size="pageSize"
              :total="fakeTotal"
              @current-change="changePage"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" title="活动详情" width="640px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="活动ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="活动名称">{{ detail.activityName }}</el-descriptions-item>
        <el-descriptions-item label="活动类型">{{ detail.activityType }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.endTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="规则描述">{{ detail.ruleDesc }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createActivity, getActivityDetail, getActivityList } from '../api/activity'

const formRef = ref()
const page = ref(1)
const pageSize = ref(10)
const fakeTotal = ref(0)
const tableData = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const form = reactive({
  activityName: '',
  activityType: '',
  startTime: '',
  endTime: '',
  status: 1,
  ruleDesc: ''
})

const rules = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  activityType: [{ required: true, message: '请输入活动类型', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function loadList() {
  const res = await getActivityList({ page: page.value, pageSize: pageSize.value })
  tableData.value = res.data || []
  fakeTotal.value = page.value * pageSize.value + (tableData.value.length === pageSize.value ? pageSize.value : 0)
}

async function submitForm() {
  await formRef.value.validate()
  await createActivity(form)
  ElMessage.success('活动创建成功')
  resetForm()
  page.value = 1
  await loadList()
}

function resetForm() {
  form.activityName = ''
  form.activityType = ''
  form.startTime = ''
  form.endTime = ''
  form.status = 1
  form.ruleDesc = ''
  formRef.value?.clearValidate()
}

async function showDetail(id) {
  const res = await getActivityDetail(id)
  detail.value = res.data
  detailVisible.value = true
}

function changePage(nextPage) {
  page.value = nextPage
  loadList()
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

onMounted(loadList)
</script>

<style scoped>
.full-width {
  width: 100%;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.pager {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
