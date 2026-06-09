<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <div class="brand-title">Activity Agent</div>
        <div class="brand-subtitle">活动运营数据分析演示</div>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="#d8e3ff"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/activity">活动管理</el-menu-item>
        <el-menu-item index="/participate">用户参与</el-menu-item>
        <el-menu-item index="/reward">奖励发放</el-menu-item>
        <el-menu-item index="/statistics">统计查询</el-menu-item>
        <el-menu-item index="/agent">Agent 查询</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ pageTitle }}</div>
        <div class="header-subtitle">SpringBoot + Python Agent 实习项目演示界面</div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const titleMap = {
  '/': '首页概览',
  '/activity': '活动管理',
  '/participate': '用户参与',
  '/reward': '奖励发放',
  '/statistics': '统计查询',
  '/agent': 'Agent 查询'
}

const pageTitle = computed(() => titleMap[route.path] || '活动运营数据分析 Agent')
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(31, 77, 145, 0.18), transparent 30%),
    linear-gradient(180deg, #f7f9fd 0%, #eef3f8 100%);
}

.aside {
  background: linear-gradient(180deg, #112846 0%, #1a4675 100%);
  color: #fff;
  box-shadow: 6px 0 24px rgba(17, 40, 70, 0.18);
}

.brand {
  padding: 26px 22px 18px;
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.brand-subtitle {
  margin-top: 8px;
  color: #b6c9ef;
  font-size: 13px;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item) {
  margin: 6px 12px;
  border-radius: 10px;
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.16);
}

.header {
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(17, 40, 70, 0.08);
}

.header-title {
  font-size: 24px;
  font-weight: 700;
  color: #16233b;
}

.header-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #70809a;
}

.main {
  padding: 24px;
}
</style>
