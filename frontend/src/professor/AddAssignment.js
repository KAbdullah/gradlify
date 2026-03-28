/**
 * This component allows professors to create new assignments
 * and associate them with existing courses.
 *
 * Professors can:
 * - Select a course from their course list
 * - Choose an assignment type (lab, quiz, project, etc.)
 * - Enter a name for the assignment
 *
 * The component sends a POST request to `/api/professor/assignments` on submission,
 * which creates a new assignment in the database.
 */

import React, { useEffect, useState } from "react";
import axios from "axios";
import './FormStyles.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircleCheck, faCircleExclamation } from '@fortawesome/free-solid-svg-icons';

function AddAssignment() {
  const [courses, setCourses] = useState([]);
  const [selectedCourse, setSelectedCourse] = useState("");
  const [assignmentName, setAssignmentName] = useState("");
  const [assignmentType, setAssignmentType] = useState("");
  const [message, setMessage] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);

  /**
     * On component mount, fetch the professor's course list.
     * These are used to populate the "Course" dropdown.
  */
  useEffect(() => {
    axios.get("/api/professor/courses")
      .then(res => setCourses(res.data))
      .catch(err => console.error(err));
  }, []);

  /**
     * Form submission handler.
     * Sends the assignment name, type, and selected course ID to the backend.
  */
  const handleSubmit = () => {
    axios.post("/api/professor/assignments", {
      name: assignmentName,
      type: assignmentType,
      courseId: selectedCourse
    }).then(() => {
      // Clear form and show success message
      setMessage("Assignment added successfully!");
      setIsSuccess(true);
      setAssignmentName('');
      setSelectedCourse('');
      setAssignmentType('lab');
    }).catch(err => {
      console.error(err);
      setMessage("Failed to add assignment.");
      setIsSuccess(false);
    });
  };

  return (
    <div className="form-container">
      <h2 className="form-title">Add Assignment</h2>
      <form className="styled-form" onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
        <div className="form-group">
          <label>Course</label>
          <select value={selectedCourse} onChange={(e) => setSelectedCourse(e.target.value)} required>
            <option value="">-- Select a course --</option>
            {courses.map(course => (
              <option key={course.id} value={course.id}>
                {course.code} - {course.name}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Type</label>
          <select value={assignmentType} onChange={(e) => setAssignmentType(e.target.value)} required>
            <option value="">-- Select type of the assignment --</option>
            <option value="lab">Lab</option>
            <option value="quiz">Quiz</option>
            <option value="project">Project</option>
            <option value="other">Other</option>
          </select>
        </div>

        <div className="form-group">
          <label>Assignment Name</label>
          <input
            type="text"
            value={assignmentName}
            onChange={(e) => setAssignmentName(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="submit-button">Add</button>
      </form>

      {message && (
        <p className={`submit-message ${isSuccess ? 'success' : 'error'}`}>
          <FontAwesomeIcon
            icon={isSuccess ? faCircleCheck : faCircleExclamation}
            style={{ marginRight: '8px' }}
          />
          {message}
        </p>
      )}
    </div>
  );
}

export default AddAssignment;
