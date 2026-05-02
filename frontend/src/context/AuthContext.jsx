import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import axios from 'axios';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    try {
      const token    = localStorage.getItem('concern_token');
      const username = localStorage.getItem('concern_username');
      const role     = localStorage.getItem('concern_role');
      if (token && username && role) {
        // Set header immediately so API calls on first render already carry the token
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        return { token, username, role };
      }
    } catch (_) {}
    return null;
  });

  // Ref so the 401 interceptor always calls the latest logout without stale closure
  const logoutRef = useRef(null);

  const logout = () => {
    localStorage.removeItem('concern_token');
    localStorage.removeItem('concern_username');
    localStorage.removeItem('concern_role');
    delete axios.defaults.headers.common['Authorization'];
    setAuth(null);
  };
  logoutRef.current = logout;

  // Register 401 interceptor once
  useEffect(() => {
    const id = axios.interceptors.response.use(
      (res) => res,
      (err) => {
        // Don't auto-logout on failed login attempts
        const url = err.config?.url ?? '';
        if (err.response?.status === 401 && !url.includes('/api/auth/login')) {
          logoutRef.current?.();
        }
        return Promise.reject(err);
      }
    );
    return () => axios.interceptors.response.eject(id);
  }, []);

  const login = async (username, password) => {
    const res = await axios.post('/api/auth/login', { username, password });
    const { token, role } = res.data;

    // Set header BEFORE setAuth so Dashboard's first fetchData already has it
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

    localStorage.setItem('concern_token',    token);
    localStorage.setItem('concern_username', username);
    localStorage.setItem('concern_role',     role);

    setAuth({ token, username, role });
    return role;
  };

  return (
    <AuthContext.Provider value={{ auth, login, logout, isAdmin: auth?.role === 'ADMIN' }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
