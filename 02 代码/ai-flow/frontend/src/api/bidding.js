import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1/bidding',
  timeout: 120000
})

export function checkBidding(formData) {
  return api.post('/check', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getReport(reportId) {
  return api.get(`/report/${reportId}`)
}

export default {
  checkBidding,
  getReport
}
