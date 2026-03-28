import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';

export default function CourseDetailsPage() {
  const { courseId } = useParams();
  const [assignments, setAssignments] = useState([]);
  const [testCasesByAssignment, setTestCasesByAssignment] = useState({});
  const [statusMessage, setStatusMessage] = useState('');

  const [editId, setEditId] = useState(null);
  const [editDescription, setEditDescription] = useState('');
  const [editDifficulty, setEditDifficulty] = useState('');
  const [editVisible, setEditVisible] = useState(false);
  const [editFile, setEditFile] = useState(null);

  const loadCourseData = useCallback(async () => {
    try {
      const assignmentRes = await axios.get(`/api/professor/assignments/by-course/${courseId}`);
      const assignmentList = assignmentRes.data || [];
      setAssignments(assignmentList);

      const testCaseResults = await Promise.all(
        assignmentList.map(async (assignment) => {
          const res = await axios.get(`/api/professor/testcases/by-assignment/${assignment.id}`);
          return [assignment.id, res.data || []];
        })
      );

      setTestCasesByAssignment(Object.fromEntries(testCaseResults));
    } catch {
      setStatusMessage('Could not load course details.');
    }
  }, [courseId]);

  useEffect(() => {
    loadCourseData();
  }, [loadCourseData]);

  const beginEdit = (testCase) => {
    setEditId(testCase.id);
    setEditDescription(testCase.description || '');
    setEditDifficulty(testCase.difficulty || '');
    setEditVisible(!!testCase.visibleToStudents);
    setEditFile(null);
  };

  const saveEdit = async (assignmentId) => {
    try {
      const formData = new FormData();
      formData.append('description', editDescription);
      formData.append('difficulty', editDifficulty);
      formData.append('visibleToStudents', editVisible);
      if (editFile) formData.append('file', editFile);

      await axios.put(`/api/professor/testcases/${editId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setStatusMessage('Test case updated.');
      setEditId(null);
      const refreshed = await axios.get(`/api/professor/testcases/by-assignment/${assignmentId}`);
      setTestCasesByAssignment(prev => ({ ...prev, [assignmentId]: refreshed.data || [] }));
    } catch (error) {
      setStatusMessage(error?.response?.data || 'Failed to update test case.');
    }
  };

  const deleteTestCase = async (assignmentId, testCaseId) => {
    if (!window.confirm('Delete this test case?')) return;
    try {
      await axios.delete(`/api/professor/testcases/${testCaseId}`);
      setTestCasesByAssignment(prev => ({
        ...prev,
        [assignmentId]: (prev[assignmentId] || []).filter(t => t.id !== testCaseId)
      }));
      setStatusMessage('Test case deleted.');
    } catch (error) {
      setStatusMessage(error?.response?.data || 'Failed to delete test case.');
    }
  };

  return (
    <div>
      <h2>Course Details</h2>
      {!!statusMessage && <p className="submit-message">{statusMessage}</p>}

      {assignments.map(assignment => (
        <div key={assignment.id} className="assignment-block">
          <h4>{assignment.name}</h4>
          {(testCasesByAssignment[assignment.id] || []).length === 0 ? (
            <p className="muted-text">No test cases yet.</p>
          ) : (
            <ul>
              {(testCasesByAssignment[assignment.id] || []).map(testCase => (
                <li key={testCase.id} className="test-case-item details-item">
                  {editId === testCase.id ? (
                    <div className="test-case-editor">
                      <input value={editDescription} onChange={e => setEditDescription(e.target.value)} placeholder="Description" />
                      <input value={editDifficulty} onChange={e => setEditDifficulty(e.target.value)} placeholder="Difficulty" />
                      <label>
                        <input type="checkbox" checked={editVisible} onChange={e => setEditVisible(e.target.checked)} /> Visible to students
                      </label>
                      <input type="file" accept=".java" onChange={e => setEditFile(e.target.files?.[0] || null)} />
                      <div>
                        <button type="button" className="course-expand-btn" onClick={() => saveEdit(assignment.id)}>Save</button>
                        <button type="button" className="secondary-action-btn" onClick={() => setEditId(null)}>Cancel</button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div>
                        <strong>{testCase.description}</strong>
                        <div className="muted-text">Difficulty: {testCase.difficulty || 'N/A'}</div>
                        <div className="muted-text">{testCase.visibleToStudents ? 'Visible' : 'Hidden'}</div>
                      </div>
                      <div className="test-case-actions">
                        <button type="button" className="secondary-action-btn" onClick={() => beginEdit(testCase)}>Edit</button>
                        <button type="button" className="danger-action-btn" onClick={() => deleteTestCase(assignment.id, testCase.id)}>Delete</button>
                      </div>
                    </>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      ))}
    </div>
  );
}
