import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode
} from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import http from '../services/HttpClient.ts';

export type User = {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  role: string;
};

export type AuthContextType = {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
  login: (username: string, password: string, firstName?: string, lastName?: string) => Promise<boolean>;
  register: (username: string, password: string, firstName: string, lastName: string) => Promise<boolean>;
  logout: () => void;
};

type AuthProviderProps = {
  children: ReactNode;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: AuthProviderProps): JSX.Element {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const checkAuth = async () => {
      setIsLoading(true);
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          setIsLoading(false);
          return;
        }
        // Set token in HTTP client // attach token to headers
        http.setAuthToken(token);
        const response = await http.get('/auth/me');
        if (response.status === 200) {
          setUser(response.data);
        } else {
          localStorage.removeItem('token');
          http.setAuthToken(null);
        }
      } catch (error) {
        console.error('Authentication check failed', error);
        localStorage.removeItem('token');
        http.setAuthToken(null);
      } finally {
        setIsLoading(false);
      }
    };
    checkAuth();
  }, []);

  // TODO firstName?: string, lastName?: are creating hard-dependencies (yes ik they are optional)
  const login = useCallback(async (username: string, password: string, firstName?: string, lastName?: string): Promise<boolean> => {
    setIsLoading(true);
    try {
      const response = await http.post('/auth/login', { username, password, firstName, lastName });
      if (response.status === 200 && response.data.token) {
        localStorage.setItem('token', response.data.token);
        
        http.setAuthToken(response.data.token);
        
        setUser(response.data.user);
        
        const redirectTo = new URLSearchParams(location.search).get('redirect');
        navigate(redirectTo || '/', { replace: true });

        return true;
      }
      
      return false;
    } catch (error) {
      console.error('Login failed', error);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [navigate, location.search]);

  const register = useCallback(async (username: string, password: string, firstName: string, lastName: string): Promise<boolean> => {
    setIsLoading(true);
    
    try {
      console.log(username, password, firstName, lastName);
      const response = await http.post('/auth/register', { username, password, firstName, lastName });
      
      if (response.status === 201 && response.data.token) {
        localStorage.setItem('token', response.data.token);
        http.setAuthToken(response.data.token);
        setUser(response.data.user);
        const redirectTo = new URLSearchParams(location.search).get('redirect');
        navigate(redirectTo || '/', { replace: true });
        return true;
      }
      return false;
    } catch (error) {
      console.error('Registration failed', error);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [navigate, location.search]);

  const logout = useCallback(() => {
    setUser(null);
    localStorage.removeItem('token');
    http.setAuthToken(null);
    navigate('/login', { replace: true });
  }, [navigate]);

  // Memoize the context value to prevent unnecessary re-renders
  const contextValue: AuthContextType = {
    isAuthenticated: !!user,
    isLoading,
    user,
    login,
    register,
    logout
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
