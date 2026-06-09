/**
 * Dashboard Service
 * Handles all dashboard-related API calls
 */

import { api } from '@/services/http/api'
import type { Dashboard } from '@/types/api'

class DashboardService {
  /**
   * Fetch dashboard data for authenticated user
   * @returns Dashboard data with user profile and assessment summary
   */
  async getDashboard(): Promise<Dashboard.Response> {
    const response = await api.get<Dashboard.Response>('/dashboard')
    return response.data
  }

}

export const dashboardService = new DashboardService()
