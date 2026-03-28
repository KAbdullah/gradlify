/**
 *
 * This React component allows professors to bulk-invite students to a selected course
 * by uploading a properly formatted CSV file. It fetches the list of courses from the
 * backend (`/api/professor/courses`), allows the user to select one, and then submits
 * the CSV to the `/api/professor/bulk-invite` endpoint via a multipart/form-data POST request.
 *
 * CSV Requirements:
 * - Must contain the following columns: Email, First Name, Last Name
 *
 */


import React, { useState, useEffect } from 'react';
import axios from 'axios';

export default function InviteStudent() {
  const [file, setFile] = useState(null);
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [message, setMessage] = useState('');
  const [invitedUsers, setInvitedUsers] = useState([]);


     // Fetch all courses when component mounts
    useEffect(() => {
      axios.get('/api/professor/courses')
        .then(res => setCourses(res.data))
        .catch(err => console.error(err));
    }, []);

    useEffect(() => {
      if (selectedCourseId) {
        axios.get(`/api/professor/invited/${selectedCourseId}`)
          .then(res => setInvitedUsers(res.data))
          .catch(err => console.error(err));
      }
    }, [selectedCourseId]);


  // Handle file input selection
  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
    setMessage('');
  };

  const handleUninvite = async (email) => {
    try {
      await axios.delete(`/api/professor/uninvite/${selectedCourseId}/${email}`);
      setInvitedUsers(prev => prev.filter(user => user.email !== email));
    } catch (err) {
      console.error(err);
      setMessage('Failed to uninvite student.');
    }
  };


  // Handle form submission to upload CSV and invite students
  const handleUpload = async () => {
      if (!file || !selectedCourseId) {
        setMessage('Please select a course and upload a CSV file.');
        return;
      }


    const formData = new FormData();
    formData.append('file', file);
    formData.append('courseId', selectedCourseId);

    // POST request to backend endpoint for bulk student invites
    try {
      const response = await axios.post('/api/professor/bulk-invite', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      setMessage(response.data);
      setFile(null);
    } catch (error) {
      setMessage(error.response?.data || 'Failed to upload and invite students.');
    }
  };

 return (
   <div className="form-container">
     <h2 className="form-title">Invite Students</h2>
     <p>Select a course and upload a CSV with the following columns:</p>
     <ul>
       <li><strong>Email</strong></li>
       <li><strong>First Name</strong></li>
       <li><strong>Last Name</strong></li>
     </ul>

     <form className="styled-form" onSubmit={(e) => e.preventDefault()}>
       <div className="form-group">
         <label>Select Course</label>
         <select
           value={selectedCourseId}
           onChange={(e) => setSelectedCourseId(e.target.value)}
         >
           <option value="">-- Select Course --</option>
           {courses.map(course => (
             <option key={course.id} value={course.id}>
               {course.code} - {course.name} ({course.term} {course.year})
             </option>
           ))}
         </select>
       </div>

       <div className="form-group upload-section">
         <label className="upload-label" htmlFor="upload-file">Choose CSV File</label>
         <input id="upload-file" type="file" accept=".csv" className="upload-input" onChange={handleFileChange} />
         {file && (
           <div className="uploaded-files">
             <ul><li>{file.name}</li></ul>
           </div>
         )}
       </div>

       <div style={{ marginTop: '2rem' }}>
         <button className="submit-button" onClick={handleUpload}>Upload & Invite</button>
       </div>

       {message && (
         <div className={`submit-message ${message.includes('Failed') ? 'error' : 'success'}`}>
           {message}
         </div>
       )}
     </form>

      {invitedUsers.length > 0 && (
        <div className="form-group invited-users">
          <h3 className="section-title">Invited Students (Pending Registration)</h3>
          <div className="invited-list">
            {invitedUsers.map(user => (
              <div key={user.email} className="invited-card">
                <span className="invited-info">
                  {user.firstName} {user.lastName} <span className="invited-email">({user.email})</span>
                </span>
                <button
                  className="uninvite-button"
                  onClick={() => handleUninvite(user.email)}
                >
                  Uninvite
                </button>
              </div>
            ))}
          </div>
        </div>
      )}


   </div>
 );
}