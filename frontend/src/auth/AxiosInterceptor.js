// axiosSetup.js
import axios from 'axios';

/**
 * This file sets up a global Axios interceptor to automatically attach
 * the JWT token to every outgoing HTTP request.
 *
 * Used across the entire Gradify frontend to ensure secure communication
 *    with backend API endpoints (e.g., /api/student/courses).
 *
 * The token is stored in localStorage after login/registration and
 *    injected into the Authorization header for authenticated requests.
 *
 * This interceptor makes it unnecessary to manually include the token
 * in every Axios call — improving modularity and reducing boilerplate.
 */


axios.interceptors.request.use(
  config => {
   // Retrieve JWT token from browser localStorage
    const token = localStorage.getItem('token');

    // If token exists, attach it to the Authorization header
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

