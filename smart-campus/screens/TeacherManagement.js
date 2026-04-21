import React, { useState } from 'react';
import TeacherModal from './TeacherModal';
import { api } from '../services/api';

export default function TeacherManagement() {
  const [modalOpen, setModalOpen] = useState(false);
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchTeachers = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/teachers');
      setTeachers(res.data);
    } finally {
      setLoading(false);
    }
  };

  React.useEffect(() => { fetchTeachers(); }, []);

  return (
    <div>
      <h1>Teacher Management</h1>
      <button onClick={() => setModalOpen(true)}>+ Add Teacher</button>
      <TeacherModal open={modalOpen} onClose={() => setModalOpen(false)} onSuccess={fetchTeachers} />
      {loading ? <div>Loading...</div> : (
        <div className="teacher-list">
          {teachers.map(t => (
            <div key={t.id} className="teacher-card">
              <div className="teacher-name">{t.fullName}</div>
              <div className="teacher-email">{t.email}</div>
              <div className="teacher-meta">{t.department} | {t.designation}</div>
              <div className={`status-badge ${t.status === 'ACTIVE' ? 'active' : 'inactive'}`}>{t.status}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
