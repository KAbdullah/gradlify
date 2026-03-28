/**
 * This component handles user authentication for both professors and students.
 * It provides a login form, stores the JWT token and role on success,
 * and redirects users to the appropriate dashboard based on their role.
 *
 * It also includes automatic redirection for users who are already logged in
 * (i.e., token and role already stored in localStorage and token is still valid).
 */

import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './FormStyles.css';
import { hardLogout } from './Logout';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();


   /**
   * Handle login form submission.
   * Sends username and password to the backend.
   * On success, saves token and role in localStorage and navigates to the dashboard.
   */
  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post('/api/auth/login', { username, password });
      // Save token and user role to localStorage for future authenticated requests
      localStorage.setItem('token', res.data.token);
      localStorage.setItem('role', res.data.role);

      // Redirect based on role
      if (res.data.role === 'PROFESSOR') {
        navigate('/professor/dashboard');
      } else {
        navigate('/student/dashboard');
      }
    } catch (err) {
      // If login fails (e.g., 401), display error message
      setError('Invalid username or password');
    }
  };


  /**
    * On component load, check if a token is already present in localStorage.
    * If so, validate the token with the backend:
    *   - If valid: redirect the user to their dashboard.
    *   - If invalid: clear token and role (hardLogout) and stay on login page.
  */
  useEffect(() => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    if (!token) return; // no token = user is not logged in

    // Verify token validity with backend
    axios.get('/api/auth/validate')
      .then(() => {
        if (role === 'STUDENT') navigate('/student/dashboard');
        if (role === 'PROFESSOR') navigate('/professor/dashboard');
      })
      .catch(() => {
        // Token invalid, clear and stay on login page
        hardLogout();
      });
  }, []);




  return (
    <div className="auth-form">
      <h2>Login</h2>
      {error && <p className="error">{error}</p>}
      <form onSubmit={handleLogin}>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <button type="submit">Login</button>
      </form>

      <p style={{ textAlign: 'right', marginTop: '10px' }}>
        <a href="/forgot-password" style={{ color: '#007bff', textDecoration: 'underline' }}>
          Forgot Password?
        </a>
      </p>



      <p style={{ marginTop: '12px' }}>
        Don't have an account? <a href="/signup">Sign up</a>
      </p>
    </div>
  );
};

export default Login;
