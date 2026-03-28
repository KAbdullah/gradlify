/**
 * This is the main application router for the Gradify platform.
 * It defines the top-level routes and handles user access control based on role.
 *
 * Routes are protected using the <RequireRole> component to enforce role-based access:
 * - Professors are routed to the ProfessorMenu
 * - Students are routed to the StudentPage
 *
 * It also includes routes for login, password recovery, and registration via email invitation.
 * Unmatched routes redirect to the login page by default.
 */

import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';


import Login from './auth/Login';
import StudentPage from './student/StudentPage';
import ProfessorMenu from './professor/ProfessorMenu';
import ForgotPassword from './auth/ForgotPassword';
import ResetForgottenPassword from './auth/ResetForgottenPassword'; // /reset-password?token=...
import SetPasswordPage from './auth/SetPasswordPage';
import Signup from './auth/Signup';
import { RequireRole } from './auth/RequireRole';
import './auth/AxiosInterceptor';


function App() {
  return (
    <Router>
      <Routes>
        {/* Public */}

        <Route path="/" element={<Login />} />
        <Route path="/student/dashboard" element={ <RequireRole role="STUDENT"> <StudentPage /> </RequireRole> } />
        <Route path="/professor/dashboard/*" element={ <RequireRole role="PROFESSOR"> <ProfessorMenu /> </RequireRole> } />
        <Route path="/professor/*" element={ <RequireRole role="PROFESSOR"> <ProfessorMenu /> </RequireRole> } />
        <Route path="/student/*" element={ <RequireRole role="STUDENT"> <StudentPage /> </RequireRole> } />
        {/* Forgot-password flow (link from email): /reset-password?token=... */}
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetForgottenPassword />} />
        <Route path="/signup" element={<Signup />} />
        {/* Early-registration (invite) flow: /set-password?token=... */}
        <Route path="/set-password" element={<SetPasswordPage />} />
        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
