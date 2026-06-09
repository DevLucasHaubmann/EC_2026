import { api } from '@/services/http/api'
import type { AssessmentData, AdminAssessmentRequest, Page } from '@/types/admin'

export const adminAssessmentService = {
  findAll: async (page = 0, size = 10): Promise<Page<AssessmentData>> => {
    const res = await api.get<Page<AssessmentData>>(`/assessments?page=${page}&size=${size}`)
    return res.data
  },

  findById: async (id: number): Promise<AssessmentData> => {
    const res = await api.get<AssessmentData>(`/assessments/${id}`)
    return res.data
  },

  findByUserId: async (userId: number): Promise<AssessmentData> => {
    const res = await api.get<AssessmentData>(`/assessments/users/${userId}`)
    return res.data
  },

  update: async (id: number, data: AdminAssessmentRequest): Promise<AssessmentData> => {
    const res = await api.put<AssessmentData>(`/assessments/${id}`, data)
    return res.data
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/assessments/${id}`)
  },

  create: async (userId: number, data: AdminAssessmentRequest): Promise<AssessmentData> => {
    const res = await api.post<AssessmentData>(`/assessments/users/${userId}`, data)
    return res.data
  },
}
