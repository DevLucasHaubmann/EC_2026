import { api } from '../http/api'

export const adminAssessmentService = {
  findByUserId: async (userId: number) => {
    const res = await api.get(`/assessments/users/${userId}`)
    return res.data
  },

  update: async (id: number, data: object) => {
    const res = await api.put(`/assessments/${id}`, data)
    return res.data
  },

  delete: async (id: number) => {
    await api.delete(`/assessments/${id}`)
  },

  create: async (userId: number, data: object) => {
    const res = await api.post(`/assessments/users/${userId}`, data)
    return res.data
  },
}
