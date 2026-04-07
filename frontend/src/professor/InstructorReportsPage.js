/**
 * This component provides instructors with visual reports and analytics
 * on student performance using data directly from the backend database.
 *
 * Reports currently supported:
 *  - Average Performance (across multiple assignments)
 *  - Error Distribution (compilation, timeout, etc.)
 *  - Grade Distribution (binned histogram with summary stats)
 *  - Submission Statistics (min, max, avg for a single assignment)
 *
 * Each report pulls from a dedicated API endpoint in the `/api/professor/reports/...` namespace.
 * Data is visualized using Recharts.
 */

import { useState, useEffect } from 'react';
import axios from 'axios';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid,
  PieChart, Pie, Cell, BarChart, Bar
} from 'recharts';
import './FormStyles.css';

// Format for showing assignment titles in dropdown
const getAssignmentLabel = (a) =>
  `${a.name} – ${a.course?.code} ${a.course?.term} ${a.course?.year}`;

const COLORS = ['#8884d8', '#82ca9d', '#ffc658', '#ff6b6b', '#a29bfe'];

export default function InstructorReportsPage() {
  const [reportType, setReportType] = useState('average');
  const [averages, setAverages] = useState([]);
  const [errors, setErrors] = useState([]);
  const [distribution, setDistribution] = useState([]);
  const [stats, setStats] = useState({ median: 0, mode: 0, std: 0 });
  const [assignments, setAssignments] = useState([]);
  const [selectedAssignments, setSelectedAssignments] = useState([]);
  const [shouldShowChart, setShouldShowChart] = useState(false);
  const selectionCopy = [...selectedAssignments];
  const [submissionStats, setSubmissionStats] = useState(null);


  /**
     * Fetches the professor's assignments from the backend.
  */
  useEffect(() => {
    axios.get(`/api/professor/assignments`)
      .then(res => setAssignments(res.data))
      .catch(err => console.error("Failed to load assignments", err));
  }, []);

  /**
     * Automatically select one assignment for non-average reports
     * to simplify the UI (since they only allow one selection).
  */
  useEffect(() => {
    if (assignments.length > 0 && selectedAssignments.length === 0 && reportType !== 'average') {
      setSelectedAssignments([assignments[0].id]);
    }
  }, [assignments, reportType, selectedAssignments.length]);


  /**
     * Handle report generation request.
     * Calls appropriate endpoint depending on report type.
  */
  const handleGenerateChart = () => {
    if (selectedAssignments.length === 0) return;

    const endpoint = {
      average: '/api/professor/reports/averages/by-assignments',
      errors: '/api/professor/reports/errors/by-assignments',
      distribution: '/api/professor/reports/distribution/by-assignments'
    }[reportType];

    if (reportType === 'submissionStats') {
      axios.get(`/api/professor/reports/submission-stats/${selectionCopy[0]}`)
        .then(res => {
          setSubmissionStats(res.data);
          setShouldShowChart(true);
        })
        .catch(err => console.error("Failed to load submission stats", err));
      return;
    }

    axios.post(endpoint, selectionCopy)
      .then(res => {
        console.log(`[${reportType.toUpperCase()}] API response:`, res.data); // ← HERE


         if (reportType === 'average') {
           const enriched = res.data.map(item => ({
             ...item,
             label: item.assignmentName // simplified
           }));
           setAverages(enriched);



        } else if (reportType === 'errors') {
          setErrors(res.data);
        } else if (reportType === 'distribution') {
          const { bins, median, mode, std } = res.data;

           // Sort grade ranges numerically
          const sortedBins = [...bins].sort((a, b) => {
            const getLowerBound = (range) => {
              if (range === "100") return 100;
              return parseInt(range.split("-")[0]);
            };
            return getLowerBound(a.range) - getLowerBound(b.range);
          });

          setDistribution(sortedBins);
          setStats({ median, mode, std });
        }

        setShouldShowChart(true);
      })
      .catch(err => console.error(`Failed to load ${reportType} data`, err));
  };


    /**
     * Reset state when switching between report tabs.
    */
  const handleTabChange = (type) => {
    setReportType(type);
    setAverages([]);
    setErrors([]);
    setDistribution([]);
    setStats({ median: 0, mode: 0, std: 0 });
    setSelectedAssignments([]);
    setShouldShowChart(false);
  };

  return (
    <div className="form-container">
      <h1 className="form-title">Instructor Reports</h1>
      <form className="styled-form">
        <div className="form-group">
          <label>Select Report Type:</label>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button type="button" className={`tab-button ${reportType === 'average' ? 'active' : ''}`} onClick={() => handleTabChange('average')}>Average Performance</button>
            <button type="button" className={`tab-button ${reportType === 'errors' ? 'active' : ''}`} onClick={() => handleTabChange('errors')}>Error Distribution</button>
            <button type="button" className={`tab-button ${reportType === 'distribution' ? 'active' : ''}`} onClick={() => handleTabChange('distribution')}>Grade Distribution</button>
            <button type="button" className={`tab-button ${reportType === 'submissionStats' ? 'active' : ''}`} onClick={() => handleTabChange('submissionStats')}>Submission Stats</button>
          </div>
        </div>

        <div className="form-group">
          <select
            multiple={reportType === 'average'}
            value={selectedAssignments}
            onChange={(e) => {
              const values = Array.from(e.target.selectedOptions, option => Number(option.value));
              setSelectedAssignments(reportType === 'average' ? values : values.slice(0, 1));
              setShouldShowChart(false);
            }}
          >

            {assignments.map(a => (
              <option key={a.id} value={a.id}>
                {getAssignmentLabel(a)}
              </option>
            ))}


          </select>
          <button type="button" className="run-grading-button" onClick={handleGenerateChart}>Generate Chart</button>

        </div>
      </form>

      {reportType === 'average' && shouldShowChart && averages.length > 0 && (
        <div className="chart-container" style={{ marginTop: '30px' }}>
          <h2 className="form-subtitle">Average Grade (Selected Assignments)</h2>
          <LineChart width={500} height={300} data={averages}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="label" angle={-30} textAnchor="end" interval={0} height={70} />

            <YAxis domain={[0, 100]} />
            <Tooltip />
            <Line type="monotone" dataKey="averageGrade" stroke="#8884d8" />
          </LineChart>
        </div>
      )}

      {reportType === 'errors' && shouldShowChart && errors.length > 0 && (
        <div className="chart-container" style={{ marginTop: '30px' }}>
          <h2 className="form-subtitle">Error Type Distribution</h2>
          <PieChart width={700} height={300}>
            <Pie
              data={errors}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              outerRadius={100}
              label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
            >
              {errors.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value, name) => [`${value} error(s)`, name]} />
          </PieChart>
          <div style={{ marginTop: '40px' }}>
            <h3 className="form-subtitle">Error Count Overview</h3>

            <BarChart width={600} height={300} data={errors}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#ff6b6b" />
            </BarChart>
          </div>
        </div>
      )}

      {reportType === 'distribution' && shouldShowChart && Array.isArray(distribution) && distribution.length > 0 && (

        <div className="chart-container" style={{ marginTop: '30px' }}>
          <h2 className="form-subtitle">Grade Distribution</h2>
          <BarChart width={600} height={300} data={distribution}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="range" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="count" fill="#8884d8" />
          </BarChart>
          <div style={{ marginTop: '20px' }}>
            <strong>Summary Statistics:</strong><br />
            Median: {stats.median} <br />
            Mode: {stats.mode} <br />
            Standard Deviation: {stats.std}
          </div>
        </div>
      )}
      {reportType === 'submissionStats' && shouldShowChart && submissionStats && (
        <div className="chart-container" style={{ marginTop: '30px' }}>
          <h2 className="form-subtitle">Submission Statistics</h2>
          <ul>
            <li><strong>Average Grade:</strong> {submissionStats.average?.toFixed(2) ?? 'N/A'}</li>
            <li><strong>Maximum Grade:</strong> {submissionStats.max ?? 'N/A'}</li>
            <li><strong>Minimum Grade:</strong> {submissionStats.min ?? 'N/A'}</li>
          </ul>
        </div>
      )}

    </div>
  );
}
