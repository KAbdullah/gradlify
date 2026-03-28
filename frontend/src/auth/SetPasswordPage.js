/**
 * This component is used for users to set their password for the first time
 * after being invited (e.g., via CSV upload or manual registration).
 *
 * The invite email contains a token embedded in the URL.
 * This page extracts that token and allows the user to define a new password.
 *
 * The token and password are sent to the backend endpoint: /api/auth/set-password
 */

import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useSearchParams, useNavigate } from 'react-router-dom';

export default function SetPasswordPage() {
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const t = (searchParams.get('token') || '').trim();
    setToken(t);
  }, [searchParams]);

  /**
   * Form submission handler.
   * Sends token and new password to the backend to activate the user's account.
  */
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!token) { setMessage('Invalid or missing token.'); return; }
    if (!password) { setMessage('Please enter a password.'); return; }

    setSubmitting(true);
    setMessage('Setting password...');

    try {
    /**
     * POST request to backend with:
     * - token (in request body)
     * - password (in request body)
     * Backend will validate the token, activate the account, and set password.
     */
       const res = await axios.post('/api/auth/set-password', {
        token,
        password
      });
      // Show success message and redirect to login
      setMessage(res.data || 'Your password has been set successfully. You may now log in.');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      // Handle invalid token or backend failure
      setMessage(err.response?.data || 'Failed to set password. Try again.');
      setSubmitting(false);
    }
  };


  return (
    <div className="auth-form">
      <h2>Set Your Password</h2>
      {!token && <p className="submit-message error">Invalid or missing token in the link.</p>}
      {message && <div className={`submit-message ${message.includes('successfully') ? 'success' : 'error'}`}>{message}</div>}
      <form onSubmit={handleSubmit}>
        <input
          type="password"
          placeholder="Enter new password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={submitting || !token}
          required
        />
        <button type="submit" disabled={submitting || !token}>
          {submitting ? 'Setting…' : 'Set Password'}
        </button>
      </form>
    </div>
  );
}
