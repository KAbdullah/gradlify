/**
 * This component renders the "Forgot Password" form for users who need
 * to reset their account credentials.
 *
 * The user enters their email, which is then submitted to the backend
 * via a POST request to /api/auth/forgot-password.
 *
 * The backend is responsible for validating the email and sending a
 * password reset link if the user exists.
 */

import React, { useState } from 'react';
import axios from 'axios';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');


  /**
   * Handle form submission.
   * Validates that email is not empty before sending request.
   * Sends a POST request to the backend to initiate the password reset process.
   */
const handleSubmit = async (e) => {
  e.preventDefault();
  if (!email) {
    setMessage('Please enter your email.');
    return;
  }

  try {
    const response = await axios.post('/api/auth/forgot-password', { email });
    setMessage(response.data);
  } catch (error) {
    setMessage(error.response?.data || 'Failed to send reset email.');
  }
};


  return (
    <div className="auth-form">
      <h2>Forgot Password</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder="Enter your registered email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <button type="submit">Send Reset Link</button>
      </form>
      {message && (
        <div className={`submit-message ${message.includes('sent') ? 'success' : 'error'}`}>
          {message}
        </div>
      )}
    </div>
  );
}
