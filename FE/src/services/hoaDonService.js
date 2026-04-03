import api from './api'

const ENDPOINT = '/admin/hoa-don'

export const getOrders = (params = {}) => api.get(ENDPOINT, { params })

export const getOrderDetail = (id) => api.get(`${ENDPOINT}/${id}`)

export const updateOrderStatus = (id, status) => 
  api.put(`${ENDPOINT}/${id}/status`, null, { params: { status } })

export default {
  getOrders,
  getOrderDetail,
  updateOrderStatus,
}
