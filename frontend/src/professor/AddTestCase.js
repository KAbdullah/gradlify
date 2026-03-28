/**
 * This component allows professors to upload new test cases for a specific assignment.
 * Test cases are stored in the database and linked to assignments.
 * The uploaded file must be a `.java` file and includes optional visibility/difficulty metadata.
 *
 * Professors can:
 * - Select a course and assignment
 * - Provide a textual description of the test case
 * - Set whether the test case is visible to students
 * - Specify difficulty (only if visible)
 * - Upload the actual `.java` test file
 *
 * The form sends a multipart POST request to `/api/professor/testcases`
 */
import { useEffect, useState } from 'react';
import axios from 'axios';
import './FormStyles.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircleCheck, faCircleExclamation, faFileUpload } from '@fortawesome/free-solid-svg-icons';

export default function TestCaseUploader() {
  const [courses, setCourses] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [selectedCourse, setSelectedCourse] = useState('');
  const [selectedAssignment, setSelectedAssignment] = useState('');
  const [difficulty, setDifficulty] = useState('easy');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState(null);
  const [message, setMessage] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);
  const [visibleToStudents, setVisibleToStudents] = useState('yes'); // 'yes' | 'no'


  /**
     * On component mount, load all available courses for the logged-in professor.
     * This populates the course dropdown.
  */
  useEffect(() => {
    axios.get('/api/professor/courses')
      .then(res => setCourses(res.data))
      .catch(err => console.error(err));
  }, []);

  /**
     * Load assignments whenever a new course is selected.
     * This allows filtering assignments per course.
  */
  const loadAssignments = (courseId) => {
    setSelectedCourse(courseId);
    setSelectedAssignment('');
    setAssignments([]);
    if (courseId) {
      axios.get(`/api/professor/assignments/by-course/${courseId}`)
        .then(res => setAssignments(res.data))
        .catch(err => console.error(err));
    }
  };

  /**
     * Submits the form by uploading the test case file and metadata to the backend.
     * Validates all required fields before submission.
  */
  const handleSubmit = async () => {
    setMessage('');
    setIsSuccess(false);

    // Basic validations
    if (!selectedCourse) { setMessage('Please select a course.'); return; }
    if (!selectedAssignment) { setMessage('Please select an assignment.'); return; }
    if (!description.trim()) { setMessage('Please enter a description.'); return; }
    if (!file) { setMessage('Please select a .java file.'); return; }
    if (!file.name.toLowerCase().endsWith('.java')) { setMessage('Only .java files are allowed.'); return; }

    const visible = (visibleToStudents === 'yes');

    // Visible ⇒ difficulty REQUIRED
    if (visible && !difficulty) {
      setMessage('Please choose a difficulty for visible test cases.');
      return;
    }

    // Construct form data for multipart upload
    const formData = new FormData();
    formData.append('assignmentId', selectedAssignment);
    formData.append('description', description.trim());
    formData.append('visibleToStudents', visible);

    // Send difficulty ONLY when visible to students
    if (visible) {
      formData.append('difficulty', (difficulty || '').toLowerCase().trim());
    }

    formData.append('file', file);

    try {
      await axios.post('/api/professor/testcases', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setMessage('Test case added successfully!');
      setIsSuccess(true);

      // Reset form after successful upload
      setSelectedCourse('');
      setSelectedAssignment('');
      setAssignments([]);
      setDifficulty('easy');
      setDescription('');
      setVisibleToStudents('yes');
      setFile(null);
    } catch (error) {
      console.error('Upload error:', error);
      // Show server message (e.g., duplicate difficulty) if provided
      setMessage(error.response?.data || 'Upload failed.');
      setIsSuccess(false);
    }
  };

  return (
    <div className="form-container">
      <h2 className="form-title">Add Test Case</h2>

      <form className="styled-form" onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
        <div className="form-group">
          <label>Course</label>
          <select value={selectedCourse} onChange={e => loadAssignments(e.target.value)} required>
            <option value="">Select Course</option>
            {courses.map(c => (
              <option key={c.id} value={c.id}>
                {c.code} - {c.name} ({c.term} {c.year})
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Assignment</label>
          <select
            value={selectedAssignment}
            onChange={e => setSelectedAssignment(e.target.value)}
            required
            disabled={!selectedCourse}
          >
            <option value="">Select Assignment</option>
            {assignments.map(a => (
              <option key={a.id} value={a.id}>{a.name}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Visible to Students?</label>
          <select value={visibleToStudents} onChange={e => setVisibleToStudents(e.target.value)} required>
            <option value="yes">Yes</option>
            <option value="no">No</option>
          </select>
        </div>

        {/* Show difficulty ONLY when VISIBLE */}
        {visibleToStudents === 'yes' && (
          <div className="form-group">
            <label>Difficulty</label>
            <select value={difficulty} onChange={e => setDifficulty(e.target.value)} required>
              <option value="easy">Easy</option>
              <option value="intermediate">Intermediate</option>
              <option value="advanced">Advanced</option>
            </select>
          </div>
        )}

        <div className="form-group">
          <label>Description</label>
          <input
            type="text"
            placeholder="e.g., edge cases, test cases for grading, etc"
            value={description}
            onChange={e => setDescription(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label>Upload .java File</label>
          <div className="upload-section">
            <label htmlFor="javaUpload" className="upload-label">
              <FontAwesomeIcon icon={faFileUpload} style={{ marginRight: 8 }} />
              Choose .java File
            </label>
            <input
              type="file"
              id="javaUpload"
              accept=".java"
              onChange={e => setFile(e.target.files[0])}
              className="upload-input"
              required
            />
            {file && (
              <div className="uploaded-files">
                <strong>Selected file:</strong>
                <ul><li>{file.name}</li></ul>
              </div>
            )}
          </div>
        </div>

        <button type="submit" className="submit-button">Add Test Case</button>
      </form>

      {message && (
        <p className={`submit-message ${isSuccess ? 'success' : 'error'}`}>
          <FontAwesomeIcon icon={isSuccess ? faCircleCheck : faCircleExclamation} style={{ marginRight: 8 }} />
          {message}
        </p>
      )}
    </div>
  );
}
