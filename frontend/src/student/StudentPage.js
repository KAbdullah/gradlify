/**
* This is the main component for students to interact with the Gradify grading platform.
* It allows students to:
* - Log out securely.
* - Select a course, assignment, and difficulty level (test case).
* - Upload a Java file to be graded.
* - View grading results including passed/failed test counts.
* - View error messages and failed test output.
* - Get AI-generated feedback based on their Java code (if API key is configured).
* - Manage their Google AI API key and model name (save, update, or delete).
*
*/

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import '../student/FormStyles.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faFolderOpen } from '@fortawesome/free-solid-svg-icons';

export default function Student() {
  const [courses, setCourses] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [testCases, setTestCases] = useState([]);

  const [selectedCourse, setSelectedCourse] = useState('');
  const [selectedAssignment, setSelectedAssignment] = useState('');
  const [selectedTestCase, setSelectedTestCase] = useState('');
  const [javaFile, setJavaFile] = useState(null);
  const [result, setResult] = useState(null);
  const [isGrading, setIsGrading] = useState(false);

  const [hasKey, setHasKey] = useState(false);
  const [hasModel, setHasModel] = useState(false);
  const [apiKeyInput, setApiKeyInput] = useState('');
  const [modelNameInput, setModelNameInput] = useState('');
  const [savingKey, setSavingKey] = useState(false);
  const [saveMsg, setSaveMsg] = useState('');

  const [showSettings, setShowSettings] = useState(false);
  const [showEnrollForm, setShowEnrollForm] = useState(false);
  const [enrollCode, setEnrollCode] = useState('');
  const [enrollMsg, setEnrollMsg] = useState('');
  const [isEnrollError, setIsEnrollError] = useState(false);



  // Fetch AI settings on first render
  useEffect(() => {
     axios.get('/api/student/me/api-key')
    .then(res => {
      setHasKey(!!res.data?.hasKey);
      setHasModel(!!res.data?.hasModel);
      setModelNameInput(res.data?.modelName || '');
    })
    .catch(() => {
      setHasKey(false);
      setHasModel(false);
    });
  }, []);

  // Save student API key + model name
  const saveMyAiSettings = async () => {
    const trimmedKey = apiKeyInput.trim();
    const trimmedModel = modelNameInput.trim();

    if (!trimmedKey || !trimmedModel) {
      setSaveMsg('Please provide both API key and model name.');
      return;
    }

    setSavingKey(true);
    setSaveMsg('');
    try {
      await axios.put('/api/student/me/api-key', { apiKey: trimmedKey, modelName: trimmedModel });
      setHasKey(true);
      setHasModel(true);
      setSaveMsg('Saved.');
      setApiKeyInput('');
    } catch (e) {
      setSaveMsg('Failed to save settings.');
    } finally {
      setSavingKey(false);
    }
  };

  const removeMyAiSettings = async () => {
    setSavingKey(true);
    setSaveMsg('');
    try {
      await axios.put('/api/student/me/api-key', { apiKey: '', modelName: '' });
      setHasKey(false);
      setHasModel(false);
      setApiKeyInput('');
      setModelNameInput('');
      setSaveMsg('AI settings removed.');
    } catch (e) {
      setSaveMsg('Failed to remove settings.');
    } finally {
      setSavingKey(false);
    }
  };

   // Logout function — clears token and role, redirects to login
   const navigate = useNavigate();
     const handleLogout = () => {
       localStorage.removeItem('token');
       localStorage.removeItem('role');
       navigate('/login');
     };


  // Load all courses when component mounts
  const fetchCourses = () => {
    axios.get("/api/student/courses")
      .then(res => setCourses(res.data))
      .catch(err => console.error(err));
  };

  useEffect(() => {
    fetchCourses();
  }, []);

  const handleEnrollCourse = async () => {
    const code = enrollCode.trim();
    if (!code) {
      setEnrollMsg('Please enter a course code.');
      setIsEnrollError(true);
      return;
    }

    try {
      const res = await axios.post('/api/student/courses/enroll', { courseCode: code });
      setEnrollMsg(res.data?.message || 'Successfully enrolled in course.');
      setIsEnrollError(false);
      setEnrollCode('');
      fetchCourses();
    } catch (error) {
      setEnrollMsg(error?.response?.data?.error || 'Could not enroll with this course code.');
      setIsEnrollError(true);
    }
  };

  // Load test cases for the selected assignment
  const loadTestCases = (assignmentId) => {
    setSelectedAssignment(assignmentId);
    axios.get(`/api/student/by-assignment/${assignmentId}`)
      .then(res => setTestCases(res.data))
      .catch(err => console.error(err));
  };

  // Grade uploaded Java file using selected test case
  const handleGrading = async () => {
    if (!javaFile || !selectedTestCase) {
      alert("Please complete all fields.");
      return;
    }

    const formData = new FormData();
    formData.append("file", javaFile);
    formData.append("testCaseId", selectedTestCase);

    setIsGrading(true);
    setResult(null);

    try {
      const res = await axios.post("/api/student/grade", formData);
     setResult({
       grade: res.data.grade,
       note: res.data.note,
       aiFeedback: null,
       errors: res.data.errors || null,
       failedTests: res.data.failedTests || [],
       styleViolations: res.data.styleViolations ?? 0,
       styleDeduction: res.data.styleDeduction ?? 0,
       finalGrade: res.data.finalGrade
     });
    } catch (err) {
      console.error(err);
      setResult({ grade: "0", note: "Server error", aiFeedback: null, styleViolations: 0, styleDeduction: 0, finalGrade: 0 });
    } finally {
      setIsGrading(false);
    }
  };

  // Get AI feedback from LLM for the submitted code
  const handleAIFeedback = async () => {
    if (!javaFile || !selectedTestCase) {
      alert("Please complete all fields.");
      return;
    }

    const formData = new FormData();
    formData.append("file", javaFile);
    formData.append("testCaseId", selectedTestCase);

    setIsGrading(true);
    setResult(null);

    try {
      const res = await axios.post("/api/student/feedback", formData);
      setResult({ grade: null, note: null, aiFeedback: res.data.aiFeedback });
    } catch (err) {
      console.error(err);
      setResult({ grade: null, note: null, aiFeedback: "Could not fetch feedback." });
    } finally {
      setIsGrading(false);
    }
  };

  // Parse test case note and render grading summary with style
  const renderNote = (note) => {
    const [passedPart, totalPart] = note.split(',');
    const passed = parseInt(passedPart.split(':')[1]);
    const total = parseInt(totalPart.split(':')[1]);
    const failed = total - passed;

    return (
      <div style={{
        backgroundColor: '#e0f2e9',
        padding: '15px',
        border: '3px solid #1e7e34',
        borderRadius: '8px',
        width: '100%',
        boxSizing: 'border-box',
        color: '#14532d',
        fontWeight: 'bold'
      }}>
        <p>Final grade after style checks: {result.finalGrade} %</p>
        <p>grade : {result.grade} %</p>
        <p>style deduction: {result.styleDeduction ?? 0} point(s)</p>
        <p>style violations found: {result.styleViolations ?? 0}</p>
        <p>number of passed test cases: {passed}</p>
        <p>number of failed test cases : {failed}</p>
        <p>total number of test cases: {total}</p>
      </div>
    );
  };


return (

  <div className="form-container">

  <>
    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginBottom: '20px' }}>
      <button
        onClick={() => setShowSettings(prev => !prev)}
        className="settings-button"
      >
        {showSettings ? 'Hide Settings' : 'AI Settings'}
      </button>

      <button
        onClick={handleLogout}
        className="signout-button"
      >
        Sign Out
      </button>
    </div>

    {showSettings && (
      <div className="form-group" style={{ marginTop: '10px' }}>
        <label>AI Settings</label>
        <input
          type="password"
          placeholder="Paste your Google AI API key"
          value={apiKeyInput}
          onChange={e => setApiKeyInput(e.target.value)}
          className="styled-form-input"
        />
        <input
          type="text"
          placeholder="Enter Google model name (example: gemini-2.5-flash)"
          value={modelNameInput}
          onChange={e => setModelNameInput(e.target.value)}
          className="styled-form-input"
          style={{ marginTop: '10px' }}
        />
        <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
          <button
            type="button"
            className="submit-button"
            onClick={saveMyAiSettings}
            disabled={savingKey}
            style={{ flex: 1 }}
          >
            {savingKey ? 'Saving...' : (hasKey && hasModel) ? 'Update Settings' : 'Save Settings'}
          </button>
          <button
            type="button"
            className="submit-button"
            style={{ backgroundColor: '#6b7280', flex: 1 }}
            onClick={removeMyAiSettings}
            disabled={savingKey}
          >
            Remove Settings
          </button>
        </div>
        {saveMsg && (
          <div
            className={`submit-message ${saveMsg.includes('Failed') ? 'error' : 'success'}`}
            style={{ marginTop: 10 }}
          >
            {saveMsg}
          </div>
        )}
        <div style={{ marginTop: '6px', color: (hasKey && hasModel) ? '#16a34a' : '#b45309', fontSize: '14px' }}>
          {(hasKey && hasModel)
            ? `Google AI settings are saved (model: ${modelNameInput || 'configured'})`
            : 'Google AI settings are incomplete. AI feedback will be unavailable.'}
        </div>
      </div>
    )}


      <form className="styled-form" onSubmit={e => e.preventDefault()}>
        <div className="form-group">
          <div className="course-label-row">
            <label>Course</label>
            <button
              type="button"
              className="add-course-button"
              onClick={() => {
                setShowEnrollForm(prev => !prev);
                setEnrollMsg('');
              }}
            >
              +
            </button>
          </div>

          {showEnrollForm && (
            <div className="enroll-row">
              <input
                type="text"
                placeholder="Enter course code"
                value={enrollCode}
                onChange={(e) => setEnrollCode(e.target.value)}
              />
              <button type="button" className="submit-button" onClick={handleEnrollCourse}>
                Enroll
              </button>
            </div>
          )}

          {enrollMsg && (
            <div className={`submit-message ${isEnrollError ? 'error' : 'success'}`}>
              {enrollMsg}
            </div>
          )}

          <select value={selectedCourse} onChange={e => {
            const courseId = e.target.value;
            setSelectedCourse(courseId);
            setSelectedAssignment('');
            setSelectedTestCase('');
            setAssignments([]);
            setTestCases([]);
            if (courseId) {
              axios.get(`/api/student/by-course/${courseId}`)
                .then(res => setAssignments(res.data))
                .catch(err => console.error(err));
            }
          }}>
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
          <select value={selectedAssignment} onChange={e => loadTestCases(e.target.value)} required disabled={!selectedCourse}>
            <option value="">Select Assignment</option>
            {assignments.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>

                <div className="form-group">
                  <label>Difficulty</label>
                  <select
                    value={selectedTestCase}
                    onChange={e => setSelectedTestCase(e.target.value)}
                    required
                    disabled={!selectedAssignment}
                  >
                    <option value="">Select Difficulty</option>
                    {testCases.map(t => (
                      <option key={t.id} value={t.id}>
                        {t.difficulty ? (t.difficulty.charAt(0).toUpperCase() + t.difficulty.slice(1)) : 'Custom'}
                      </option>
                    ))}
                  </select>
                </div>


      <div className="form-group">
        <label>Upload Your Java File</label>
        <div className="upload-section">
          <label htmlFor="javaFileUpload" className="upload-label">
            <FontAwesomeIcon icon={faFolderOpen} style={{ marginRight: '8px' }} />
            Choose File
          </label>
          <input
            type="file"
            id="javaFileUpload"
            accept=".java"
            className="upload-input"
            onChange={e => setJavaFile(e.target.files[0])}
            required
          />
          {javaFile && (
            <div className="uploaded-files">
              <strong>Uploaded File:</strong> {javaFile.name}
            </div>
          )}
        </div>
      </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="submit-button" onClick={handleGrading}>Run Grading</button>
          <button className="submit-button" onClick={handleAIFeedback}>Get AI Feedback</button>
        </div>
      </form>

      {isGrading && (
        <div className="submit-message info">
          <FontAwesomeIcon icon={faSpinner} spin style={{ marginRight: '10px', color: '#1976d2' }} />
          Processing... please wait.
        </div>
      )}

      {result && (
        <div
          className="submit-message success"
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '20px',
            width: '100%',
            maxWidth: '900px',
            margin: '30px auto'
          }}
        >
          {result.note && (
            result.note.includes("Passed")
              ? renderNote(result.note)
              : (
                <div style={{
                  backgroundColor: '#fef3c7',
                  padding: '15px',
                  border: '3px solid #f59e0b',
                  borderRadius: '8px',
                  width: '100%',
                  boxSizing: 'border-box',
                  color: '#78350f',
                  fontWeight: 'bold'
                }}>
                  <p>Final grade after style checks: {result.finalGrade} %</p>
                  <p>Grade: {result.grade} %</p>
                  <p>Style deduction: {result.styleDeduction ?? 0} point(s)</p>
                  <p>Note: {result.note}</p>
                </div>
              )
          )}


          {(result.errors || result.failedTests) && (
            <div className="form-group" style={{ width: '100%' }}>
              <label><strong>Failed Test Messages:</strong></label>
              <div style={{
                backgroundColor: '#e0f2e9',
                padding: '20px',
                border: '3px solid #1e7e34',
                borderRadius: '8px',
                marginTop: '10px'
              }}>
                {(result.errors || '').split('\n').filter(line => line.trim() !== '').map((msg, index) => (
                  <div key={`error-${index}`} style={{
                    backgroundColor: '#f0fdf4',
                    border: '1px solid #34d399',
                    borderRadius: '6px',
                    padding: '10px',
                    marginBottom: '10px',
                    color: '#991b1b',
                    fontFamily: 'monospace',
                    fontSize: '14px',
                    whiteSpace: 'pre-wrap'
                  }}>
                    {msg.trim()}
                  </div>
                ))}
              </div>
            </div>
          )}


          {result.aiFeedback && (
            <div className="form-group" style={{ width: '100%' }}>
              <label><strong>Feedback by AI:</strong></label>
              <p style={{ whiteSpace: 'pre-wrap' }}>{result.aiFeedback}</p>
            </div>
          )}
        </div>
      )}

   </>

     </div>
   );

 }
