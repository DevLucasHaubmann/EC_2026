/**
 * Dashboard Service
 * Handles all dashboard-related API calls
 */

import { api } from '../http/api'
import type { Dashboard, Recommendation } from '@/types/api'

class DashboardService {
  /**
   * Fetch dashboard data for authenticated user
   * @returns Dashboard data with user profile and assessment summary
   */
  async getDashboard(): Promise<Dashboard.Response> {
    const response = await api.get<Dashboard.Response>('/dashboard')
    return response.data
  }

  /**
   * Fetch latest AI recommendation for the user
   * @returns Latest meal plan recommendation
   */
  async getLatestRecommendation(): Promise<Recommendation.Response | null> {
    try {
      const response = await api.get<Recommendation.Response>(
        '/ai/recommendations/me/latest'
      )
      return response.data
    } catch (error: any) {
      // If not found, return null gracefully
      if (error.response?.status === 404) {
        return null
      }
      throw error
    }
  }
}

export const dashboardService = new DashboardService()
