import http from './HttpClient.ts';
import { logger } from '../utils/logger';

const userLogger = logger.create('UserService');

export interface UserProfile {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  bio?: string;
}

export interface UserSettings {
  theme: 'light' | 'dark';
  notifications: {
    email: boolean;
    push: boolean;
    weeklyReport: boolean;
  };
}

interface ApiResponse<T> {
  success: boolean;
  error?: string;
  message?: string;
  data?: T;
}

interface UserProfileResponse extends ApiResponse<UserProfile> {
  profile?: UserProfile;
}

interface UserSettingsResponse extends ApiResponse<UserSettings> {
  settings?: UserSettings;
}

export const UserService = {

  async getCurrentUserProfile(): Promise<UserProfileResponse> {
    try {
      console.log('Fetching current user profile');
      const response = await http.get('/users/me');
      console.log('User profile response:', response.data);
      
      if (!response.data) {
        return {
          success: false,
          error: 'No data received from server'
        };
      }
      
      const profile = {
        id: String(response.data.id),
        firstName: response.data.firstName,
        lastName: response.data.lastName,
        email: response.data.email,
        bio: response.data.bio,
      };
      
      return {
        success: true,
        profile
      };
    } catch (error: any) {
      userLogger.error('Error getting user profile:', error);
      console.error('Error details:', error.response?.data || error.message);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to fetch user profile'
      };
    }
  },

  async updateUserProfile(profile: Partial<UserProfile>): Promise<ApiResponse<null>> {
    try {
      console.log('Updating user profile with data:', profile);
      const response = await http.put('/users/me', profile);
      console.log('Update profile response:', response.data);
      
      return {
        success: true,
        message: 'Profile updated successfully'
      };
    } catch (error: any) {
      userLogger.error('Error updating user profile:', error);
      console.error('Error details:', error.response?.data || error.message);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to update profile'
      };
    }
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<ApiResponse<null>> {
    try {
      console.log('Changing user password');
      const response = await http.post('/users/change-password', {
        currentPassword,
        newPassword,
      });
      console.log('Change password response status:', response.status);
      
      return {
        success: true,
        message: 'Password changed successfully'
      };
    } catch (error: any) {
      userLogger.error('Error changing password:', error);
      console.error('Error details:', error.response?.data || error.message);
      
      if (error.response?.status === 401) {
        return {
          success: false,
          error: 'Current password is incorrect'
        };
      }
      
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to change password'
      };
    }
  },

  async getUserSettings(): Promise<UserSettingsResponse> {
    try {
      console.log('Fetching user settings');
      const response = await http.get('/users/settings');
      console.log('User settings response:', response.data);
      
      if (!response.data) {
        return {
          success: false,
          error: 'No settings data received from server'
        };
      }
      
      const settings = {
        theme: response.data.theme || 'light',
        notifications: {
          email: response.data.notifications?.email ?? true,
          push: response.data.notifications?.push ?? true,
          weeklyReport: response.data.notifications?.weeklyReport ?? true,
        },
      };
      
      return {
        success: true,
        settings
      };
    } catch (error: any) {
      userLogger.error('Error getting user settings:', error);
      console.error('Error details:', error.response?.data || error.message);
      
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to fetch user settings',
        settings: {
          theme: 'light',
          notifications: {
            email: true,
            push: true,
            weeklyReport: true,
          },
        }
      };
    }
  },

  async updateUserSettings(settings: Partial<UserSettings>): Promise<ApiResponse<null>> {
    try {
      console.log('Updating user settings with data:', settings);
      const response = await http.put('/users/settings', settings);
      console.log('Update settings response:', response.data);
      
      return {
        success: true,
        message: 'Settings updated successfully'
      };
    } catch (error: any) {
      userLogger.error('Error updating user settings:', error);
      console.error('Error details:', error.response?.data || error.message);
      
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to update settings'
      };
    }
  },
};
