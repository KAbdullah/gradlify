/**
 * Role-based route protection for the Gradify frontend.
 *
 * This component is used to wrap protected routes (e.g., professor or student dashboards)
 * and ensure that only users with the correct role—and a valid token—can access them.
 *
 * If the user is not authenticated or does not have the required role,
 * they are redirected to the login page.
 *
 */

import { Navigate } from 'react-router-dom';

export function RequireRole({ children, role }) {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('role');

   // If no token or role mismatch, block access and redirect to login
  if (!token || userRole !== role) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
