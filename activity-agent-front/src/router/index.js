import { createRouter, createWebHistory } from 'vue-router'

import Dashboard from '../views/Dashboard.vue'
import ActivityManage from '../views/ActivityManage.vue'
import Participate from '../views/Participate.vue'
import RewardSend from '../views/RewardSend.vue'
import Statistics from '../views/Statistics.vue'
import AgentQuery from '../views/AgentQuery.vue'

const routes = [
  { path: '/', component: Dashboard },
  { path: '/activity', component: ActivityManage },
  { path: '/participate', component: Participate },
  { path: '/reward', component: RewardSend },
  { path: '/statistics', component: Statistics },
  { path: '/agent', component: AgentQuery }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
