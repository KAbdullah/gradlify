/**
 * This component is the main dashboard layout for professors in the Gradify system.
 * It renders navigation links to different professor-only tools and configures routing
 * for each subpage (e.g., Add Course, Add Assignment, Grade, Reports, etc.).
  */

import React from 'react';
import { Link, Routes, Route, useNavigate } from 'react-router-dom';

// Import all professor-only subpages
import AddCourse from './AddCourse';
import AddAssignment from './AddAssignment';
import AddTestCase from './AddTestCase';
import GradePage from './GradePage';
import InstructorReportsPage from './InstructorReportsPage';
import InviteStudent from './InviteStudent';
import ProfessorDashboard from './ProfessorDashboard';
import CourseDetailsPage from './CourseDetailsPage';
import './ProfessorMenu.css';

export default function ProfessorMenu() {
  const navigate = useNavigate();


  // Function to log out the professor
  const handleLogout = () => {
    // Clear auth token and role from browser storage and redirect user to login page
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    navigate('/login');
  };

  return (
    <div className="professor-container">
      <header className="professor-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Professor Dashboard</h1>
        <button
          onClick={handleLogout}
          style={{
            padding: '8px 16px',
            backgroundColor: '#dc2626',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer'
          }}
        >
          Sign Out
        </button>
      </header>

      <nav className="professor-nav">
        <Link to="/professor">Dashboard</Link>
        <Link to="/professor/add-course">Add Course</Link>
        <Link to="/professor/add-assignment">Add Assignment</Link>
        <Link to="/professor/add-test-case">Add Test Case</Link>
        <Link to="/professor/grade-submissions">Grade Submissions</Link>
        <Link to="/professor/reports">Reports</Link>
        <Link to="/professor/invite-student">Invite Student</Link>
      </nav>

      <main className="professor-main">
        <Routes>
          <Route index element={<ProfessorDashboard />} />
          <Route path="add-course" element={<AddCourse />} />
          <Route path="add-assignment" element={<AddAssignment />} />
          <Route path="add-test-case" element={<AddTestCase />} />
          <Route path="grade-submissions" element={<GradePage />} />
          <Route path="reports" element={<InstructorReportsPage />} />
          <Route path="invite-student" element={<InviteStudent />} />
          <Route path="course/:courseId" element={<CourseDetailsPage />} />
        </Routes>
      </main>
    </div>
  );
}
