import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function ProfessorDashboard() {
  const [courses, setCourses] = useState([]);
  const [statusMessage, setStatusMessage] = useState('');
  const [showHiddenCourses, setShowHiddenCourses] = useState(false);
  const navigate = useNavigate();

  const loadCourses = () => {
    axios.get('/api/professor/courses')
      .then(res => setCourses(res.data || []))
      .catch(() => setStatusMessage('Could not load courses.'));
  };

  useEffect(() => {
    loadCourses();
  }, []);

  const visibleCourses = useMemo(
    () => courses.filter(course => showHiddenCourses ? !!course.hidden : !course.hidden),
    [courses, showHiddenCourses]
  );

  const toggleCourseHidden = async (course) => {
    try {
      await axios.patch(`/api/professor/courses/${course.id}/hidden`, {
        hidden: !course.hidden
      });
      setStatusMessage(course.hidden ? 'Course is now visible.' : 'Course is now hidden.');
      loadCourses();
    } catch (error) {
      setStatusMessage(error?.response?.data || 'Failed to update course visibility.');
    }
  };

  const deleteCourse = async (course) => {
    if (!window.confirm(`Delete ${course.code} and all assignments/test cases?`)) {
      return;
    }

    try {
      await axios.delete(`/api/professor/courses/${course.id}`);
      setCourses(prev => prev.filter(c => c.id !== course.id));
      setStatusMessage('Course and associated test cases were deleted.');
    } catch (error) {
      setStatusMessage(error?.response?.data || 'Failed to delete course.');
    }
  };

  return (
    <div>
      <div className="dashboard-heading-row">
        <h2 style={{ marginTop: 0 }}>Course Overview</h2>
        <button
          type="button"
          className="secondary-action-btn"
          onClick={() => setShowHiddenCourses(prev => !prev)}
        >
          {showHiddenCourses ? 'Show Active Courses' : 'Show Hidden Courses'}
        </button>
      </div>
      {!!statusMessage && <p className="submit-message">{statusMessage}</p>}

      <div className="course-card-grid">
        {visibleCourses.map(course => (
          <div key={course.id} className="course-card square-card">
            <div className="course-card-header">
              <h3>{course.name}</h3>
              <span>{course.code}</span>
            </div>
            <p className="course-card-meta">{course.term} {course.year}</p>

            <div className="card-actions-column">
              <button type="button" className="course-expand-btn" onClick={() => navigate(`/professor/course/${course.id}`)}>
                Open Course
              </button>
              <button type="button" className="secondary-action-btn" onClick={() => toggleCourseHidden(course)}>
                {course.hidden ? 'Unhide' : 'Hide'}
              </button>
              <button type="button" className="danger-action-btn" onClick={() => deleteCourse(course)}>
                Delete Course
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
