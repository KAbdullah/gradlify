import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './FormStyles.css';

const Signup = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [role, setRole] = useState('STUDENT');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const navigate = useNavigate();

  const handleSignup = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    try {
      const payload = {
        username,
        password,
        firstName,
        lastName,
        role,
      };

      const res = await axios.post('/api/auth/signup', payload);
      localStorage.setItem('token', res.data.token);
      localStorage.setItem('role', res.data.role);

      setMessage('Signup successful. Redirecting...');
      setTimeout(() => {
        if (res.data.role === 'PROFESSOR') {
          navigate('/professor/dashboard');
        } else {
          navigate('/student/dashboard');
        }
      }, 500);
    } catch (err) {
      setError(err?.response?.data || 'Signup failed. Please try again.');
    }
  };

  return (
    <div className="auth-form">
      <h2>Create Account</h2>
      {error && <p className="error">{error}</p>}
      {message && <p>{message}</p>}

      <form onSubmit={handleSignup}>
        <input
          type="text"
          placeholder="First Name"
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          required
        />
        <input
          type="text"
          placeholder="Last Name"
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          required
        />
        <input
          type="email"
          placeholder="Email"
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

        <select
          value={role}
          onChange={(e) => setRole(e.target.value)}
          style={{ width: '100%', padding: '12px', marginBottom: '20px', borderRadius: '8px' }}
        >
          <option value="STUDENT">Sign up as Student</option>
          <option value="PROFESSOR">Sign up as Teacher</option>
        </select>

        <button type="submit">Sign Up</button>
      </form>

      <p>
        Already have an account? <a href="/">Login</a>
      </p>
    </div>
  );
};

export default Signup;
