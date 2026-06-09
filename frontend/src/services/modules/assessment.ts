import { api } from '@/services/http/api'
import type { CreateAssessmentRequest, UpdateAssessmentRequest } from '@/types/assessment'
import type { Assessment, AssessmentCreatedResponse } from '@/types/api'

export const assessmentService = {
  createOwn: async (data: CreateAssessmentRequest): Promise<AssessmentCreatedResponse> => {
    const res = await api.post<AssessmentCreatedResponse>('/assessments/me', data)
    return res.data
  },

  getOwn: async (): Promise<Assessment.Response> => {
    const res = await api.get<Assessment.Response>('/assessments/me')
    return res.data
  },

  updateOwn: async (data: UpdateAssessmentRequest): Promise<Assessment.Response> => {
    const res = await api.put<Assessment.Response>('/assessments/me', data)
    return res.data
  },
}
