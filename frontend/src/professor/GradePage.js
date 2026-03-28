/**
 * This component allows professors to upload a folder of student submissions
 * and run batch grading using a selected test case.
 *
 * Professors can:
 * - Select a course and assignment
 * - Choose a test case to grade with
 * - Upload a root folder containing multiple student subfolders
 * - Specify a result file name
 * - Trigger backend grading and download the results as a CSV
 *
 * The grading process is handled by the backend (e.g., using JUnit),
 * and the results are streamed back for display and optional download.
 */

import { useEffect, useState } from 'react';
import axios from 'axios';
import './FormStyles.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faDownload, faFolderOpen, faCircleCheck, faCircleExclamation, faSpinner } from '@fortawesome/free-solid-svg-icons';


export default function GradingPage() {
  const [courses, setCourses] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [testCases, setTestCases] = useState([]);

  const [selectedCourse, setSelectedCourse] = useState('');
  const [selectedAssignment, setSelectedAssignment] = useState('');
  const [selectedTestCase, setSelectedTestCase] = useState('');
  const [studentFolderFiles, setStudentFolderFiles] = useState([]);
  const [result, setResult] = useState('');
  const [downloadLinkVisible, setDownloadLinkVisible] = useState(false);
  const [customFileName, setCustomFileName] = useState('');
  const [rootFolderName, setRootFolderName] = useState('');
  const [isGrading, setIsGrading] = useState(false);
  const [conflictStudents, setConflictStudents] = useState([]);
  const [overwriteChoices, setOverwriteChoices] = useState({}); // { studentId: true/false }
  const [showConflictUI, setShowConflictUI] = useState(false);



  // Fetch courses on component load
  useEffect(() => {
    axios.get("/api/professor/courses")
      .then(res => setCourses(res.data))
      .catch(err => console.error(err));
  }, []);

  // Load assignments for selected course
  const loadAssignments = (courseId) => {
    setSelectedCourse(courseId);
    setSelectedAssignment('');
    setSelectedTestCase('');
    setAssignments([]);
    setTestCases([]);
    axios.get(`/api/professor/assignments/by-course/${courseId}`)
      .then(res => setAssignments(res.data))
      .catch(err => console.error(err));
  };

  // Load test cases for selected assignment
  const loadTestCases = (assignmentId) => {
    setSelectedAssignment(assignmentId);
    setSelectedTestCase('');
    setTestCases([]);
    axios.get(`/api/professor/testcases/by-assignment/${assignmentId}`)
      .then(res => setTestCases(res.data))
      .catch(err => console.error(err));
  };

    /**
     * Handles grading request:
     * - Validates that folder and test case are selected
     * - Sends all files via FormData
     * - Calls grading endpoint and displays results
     * - Shows download link on success
     */
  const handleFolderSubmit = async () => {
    if (!studentFolderFiles.length || !selectedTestCase) {
      alert('Please select a test case and upload a folder.');
      return;
    }

    try {
      // Check if grades already exist
      // Step 1: Ask backend which of these students already have grades
          const checkForm = new FormData();
          studentFolderFiles.forEach(file => checkForm.append('studentFolders', file));
          checkForm.append('testCaseId', selectedTestCase);

          const checkRes = await axios.post("/api/professor/grade/check-existing-students", checkForm);
          const alreadyGradedStudents = checkRes.data; // e.g. ["Gloria", "John"]

          if (alreadyGradedStudents.length > 0) {
            setConflictStudents(alreadyGradedStudents);

            const initialChoices = {};
            alreadyGradedStudents.forEach(s => (initialChoices[s] = false));
            setOverwriteChoices(initialChoices);

            setShowConflictUI(true);  // show checkbox UI
            return; // stop here until prof decides
          }


      const formData = new FormData();
      studentFolderFiles.forEach(file => formData.append('studentFolders', file));
      formData.append('testCaseId', selectedTestCase);
      formData.append('fileName', customFileName);


      setIsGrading(true);
      setResult('');
      setDownloadLinkVisible(false);

      const response = await axios.post("/api/professor/grade/run-folder", formData);
      const results = response.data;
      // Format result object into a readable string
      let display = '';
      for (const [student, grade] of Object.entries(results)) {
        display += `${student}:\n${grade}\n\n`;
      }
      setResult(display);
      setDownloadLinkVisible(true);
    } catch (err) {
      console.error(err);
      setResult(
        typeof err.response?.data === 'string'
          ? err.response.data
          : err.response?.data?.error || 'Error grading folder.'
      );
      setDownloadLinkVisible(false);
    } finally {
      setIsGrading(false);
    }
  };


  return (
    <div className="form-container">
      <h2 className="form-title">Grade Student Submissions</h2>
      <form className="styled-form" onSubmit={(e) => e.preventDefault()}>
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
          <select value={selectedAssignment} onChange={e => loadTestCases(e.target.value)} disabled={!selectedCourse} required>
            <option value="">Select Assignment</option>
            {assignments.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>

        <div className="form-group">
          <label>Test Case</label>
          <select value={selectedTestCase} onChange={e => setSelectedTestCase(e.target.value)} disabled={!selectedAssignment} required>
            <option value="">Select Test Case</option>
            {testCases.map(t => <option key={t.id} value={t.id}>{t.description}</option>)}
          </select>
        </div>

        {showConflictUI && conflictStudents.length > 0 && (
          <div className="conflict-section">
            <h3 className="section-title">Conflict: Already Graded Students</h3>
            <p className="conflict-description">
              The following students already have grades for this assignment.
              Please select which students you want to <b>overwrite (regrade)</b>.
              Unselected students will be skipped.
            </p>

            <div className="conflict-list">
              {conflictStudents.map(s => (
                <div key={s} className="conflict-card">
                  <label className="conflict-label">
                    <input
                      type="checkbox"
                      checked={overwriteChoices[s] || false}
                      onChange={e =>
                        setOverwriteChoices({ ...overwriteChoices, [s]: e.target.checked })
                      }
                    />
                    {s}
                  </label>
                </div>
              ))}
            </div>

            <button
              className="submit-button"
              style={{ marginTop: "15px" }}
              onClick={async () => {
                const overwriteList = Object.keys(overwriteChoices).filter(s => overwriteChoices[s]);

                const formData = new FormData();
                studentFolderFiles.forEach(file => formData.append('studentFolders', file));
                formData.append('testCaseId', selectedTestCase);
                formData.append('fileName', customFileName);
                formData.append('overwriteList', JSON.stringify(overwriteList));

                setIsGrading(true);
                setResult('');
                setDownloadLinkVisible(false);

                try {
                  const response = await axios.post("/api/professor/grade/run-folder", formData);
                  const results = response.data;
                  let display = '';
                  for (const [student, grade] of Object.entries(results)) {
                    display += `${student}:\n${grade}\n\n`;
                  }
                  setResult(display);
                  setDownloadLinkVisible(true);
                  setShowConflictUI(false);
                } catch (err) {
                  console.error(err);
                  setResult(
                    typeof err.response?.data === 'string'
                      ? err.response.data
                      : err.response?.data?.error || 'Error grading folder.'
                  );
                  setDownloadLinkVisible(false);
                } finally {
                  setIsGrading(false);
                }
              }}
            >
              Continue Grading
            </button>
          </div>
        )}



        <div className="form-group">
          <label>Grading Result File Name</label>
          <input
            type="text"
            value={customFileName}
            onChange={e => setCustomFileName(e.target.value)}
            placeholder="e.g. lab1_results"
            required
          />
        </div>

        <div className="form-group">
          <label>Upload Folder of Student Submissions</label>
          <div className="upload-section">
            <label htmlFor="folderUpload" className="upload-label">
              <FontAwesomeIcon icon={faFolderOpen} style={{ marginRight: '8px' }} />
              Choose Folder
            </label>
            <input
              type="file"
              id="folderUpload"
              webkitdirectory="true"
              directory=""
              multiple
              onChange={e => {
                const files = Array.from(e.target.files);
                setStudentFolderFiles(files);
                setResult('');
                setDownloadLinkVisible(false);

                if (files.length > 0) {
                  const fullPath = files[0].webkitRelativePath;
                  const rootFolder = fullPath.split('/')[0];
                  setRootFolderName(rootFolder);
                } else {
                  setRootFolderName('');
                }
              }}
              className="upload-input"
              required
            />
            {rootFolderName && (
              <div className="uploaded-files">
                <strong>Uploaded Folder:</strong> {rootFolderName}
              </div>
            )}
          </div>
        </div>

        {!showConflictUI && (
          <button className="submit-button" onClick={handleFolderSubmit}>
            Run Grading
          </button>
        )}
      </form>

      {isGrading && (
         <div className="submit-message info">
           <FontAwesomeIcon
             icon={faSpinner}
             spin
             style={{ marginRight: '10px', color: '#1976d2' }}
           />
           Grading in progress... please wait.
         </div>
       )}


      {result && (
        <div className={`submit-message ${downloadLinkVisible ? 'success' : 'error'}`}>
          <FontAwesomeIcon
            icon={downloadLinkVisible ? faCircleCheck : faCircleExclamation}
            style={{ marginRight: '8px' }}
          />
          {result}
        </div>
      )}

      {downloadLinkVisible && (
        <div style={{ marginTop: '10px', textAlign: 'center' }}>
          <button
            onClick={async () => {
              try {
                const token = localStorage.getItem('token'); // or however you store JWT

                const response = await axios.get(
                  "https://gradify.eecs.yorku.ca/api/professor/grade/results/download",
                  {
                    headers: {
                      Authorization: `Bearer ${token}`,
                    },
                    responseType: 'blob',
                  }
                );

                const blob = new Blob([response.data], { type: 'text/csv' });
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', `${customFileName || 'grading_results'}.csv`);
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.URL.revokeObjectURL(url);
              } catch (err) {
                console.error("Failed to download results:", err);
                alert("Download failed. Please try again.");
              }
            }}
            className="text-link"
            style={{ fontWeight: '600', color: '#2e7d32', border: 'none', background: 'none', cursor: 'pointer', textDecoration: 'underline' }}
          >
            Click here to download results
            <FontAwesomeIcon icon={faDownload} style={{ marginLeft: '8px' }} />
          </button>
        </div>
      )}

    </div>
  );
}
