import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse
} from 'axios';

class HttpClient {
  private client: AxiosInstance;
  private baseUrl: string;

  constructor() {
    this.baseUrl = 'http://localhost:8080';
    this.client = axios.create({
      baseURL: this.baseUrl,
      timeout: 300000, // 5 minutes for large file uploads // TODO: Increase hai
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    });
    this.setupInterceptors();
    const token = localStorage.getItem('token');
    if (token) {
      this.setAuthToken(token);
    }
  }

  /**
   * Set up request and response interceptors -> logging and global error handling
   */
  private setupInterceptors(): void {
    // Request interceptor for logging and error handling
    this.client.interceptors.request.use(
      (config) => {
        console.log(`[${config.method?.toUpperCase()}] ${config.url}`);
        
        // Debug auth header
        if (config.headers?.Authorization) {
          console.log('Request includes Authorization header:', 
            config.headers.Authorization.toString().substring(0, 20) + '...');
        } else {
          console.log('Request does not include Authorization header');
        }
        
        return config;
      },
      (error) => {
        console.error('Request error:', error);
        return Promise.reject(error);
      }
    );

    // Response interceptor for logging and error handling
    this.client.interceptors.response.use(
      (response) => {
        console.log(`[${response.status}] ${response.config.url}`);
        return response;
      },
      (error) => {
        if (error.response) {
          console.error(`[${error.response.status}] ${error.config.url}:`, error.response.data);
          
          // Handle 401 Unauthorized globally
          if (error.response.status === 401 && !error.config.url.includes('/auth/login')) {
            console.log('401 Unauthorized response detected, clearing token and redirecting to login');
            
            // Don't redirect for file operations (download, preview) to allow better error handling
            const isFileOperation = error.config.url.includes('/files/') && 
              (error.config.url.includes('/download') || 
               error.config.url.includes('/preview') || 
               error.config.url.includes('/thumbnail'));
            
            // Clear token
            localStorage.removeItem('token');
            
            // Only redirect for non-file operations
            if (!isFileOperation && !window.location.pathname.includes('/login')) {
              window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`;
            }
          }
        } else {
          console.error('Network or other error:', error.message);
        }
        
        return Promise.reject(error);
      }
    );
  }

  public setAuthToken(token: string | null): void {
    if (token) {
      console.log('Setting auth token:', token.substring(0, 20) + '...');
      this.client.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
      console.log('Clearing auth token');
      delete this.client.defaults.headers.common['Authorization'];
    }
  }

  public async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.get<T>(url, config);
  }

  public async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.post<T>(url, data, config);
  }

  public async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.put<T>(url, data, config);
  }

  public async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.delete<T>(url, config);
  }
}

// Create and export a singleton instance
const http = new HttpClient();
export default http;
