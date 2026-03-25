import { createRouter, createWebHistory } from 'vue-router'
import CheckWizard from '../views/CheckWizard.vue'
import HistoryList from '../views/HistoryList.vue'
import ReportDetail from '../views/ReportDetail.vue'

const routes = [
  {
    path: '/',
    name: 'CheckWizard',
    component: CheckWizard
  },
  {
    path: '/history',
    name: 'HistoryList',
    component: HistoryList
  },
  {
    path: '/report/:id',
    name: 'ReportDetail',
    component: ReportDetail
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
