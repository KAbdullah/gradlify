/**
 * This component allows professors to add new courses to the system.
 *
 * Professors can:
 * - Set the academic year (default: current year)
 * - Choose the term (Fall, Winter, or Summer)
 * - Enter the course name and course code
 *
 * The form submits a POST request to `/api/professor/courses`, which adds the course
 * to the database.
 */

import React, { useState } from 'react';
import axios from 'axios';
import './FormStyles.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircleCheck, faCircleExclamation } from '@fortawesome/free-solid-svg-icons';

function AddCourse() {
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [year, setYear] = useState(new Date().getFullYear());
  const [term, setTerm] = useState('Fall');
  const [message, setMessage] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);

  /**
     * Form submission handler.
     * Sends the course information to the backend API.
  */
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await axios.post("/api/professor/courses", { name, code, year, term });
      // Show success message and reset form
      setMessage(`Course added successfully! Share this code with students: ${code.toUpperCase()}`);
      setIsSuccess(true);
      setName('');
      setCode('');
      setYear(new Date().getFullYear());
      setTerm('Fall');
    } catch (error) {
      // Log error and show failure message
      console.error('Error adding course:', error);
      setMessage('Failed to add course.');
      setIsSuccess(false);
    }
  };

  return (
    <div className="form-container">
      <h2 className="form-title">Add Course</h2>
      <form onSubmit={handleSubmit} className="styled-form">
        <div className="form-group">
          <label>Year</label>
          <select value={year} onChange={(e) => setYear(parseInt(e.target.value))} required>
            {Array.from({ length: 11 }, (_, i) => 2020 + i).map(y => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Term</label>
          <select value={term} onChange={(e) => setTerm(e.target.value)} required>
            <option value="Fall">Fall</option>
            <option value="Winter">Winter</option>
            <option value="Summer">Summer</option>
          </select>
        </div>

        <div className="form-group">
          <label>Course Name</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label>Course Code</label>
          <input
            type="text"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
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

export default AddCourse;
