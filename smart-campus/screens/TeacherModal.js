import React, { useState } from 'react';
import { api } from '../services/api';

const steps = ["Basic Info", "Academic Details", "Assignments", "Credentials"];

export default function TeacherModal({ open, onClose, onSuccess, editTeacher }) {
  const [step, setStep] = useState(0);
  const [form, setForm] = useState({
    teacher: {
      firstName: '', lastName: '', fullName: '', email: '', phone: '', employeeId: '', department: '', designation: '', qualification: '', experienceYears: '', specialization: '', status: 'ACTIVE', dateOfJoining: ''
    },
    credentials: { username: '', password: '', passwordResetRequired: true },
    assignments: []
  });
  const [error, setError] = useState('');

  // Handlers for each step (simplified for brevity)
  const handleNext = () => setStep(s => Math.min(s + 1, steps.length - 1));
  const handlePrev = () => setStep(s => Math.max(s - 1, 0));

  const handleChange = (section, field, value) => {
    setForm(f => ({ ...f, [section]: { ...f[section], [field]: value } }));
  };

  const handleAssignmentAdd = assignment => {
    setForm(f => ({ ...f, assignments: [...f.assignments, assignment] }));
  };

  const handleSubmit = async () => {
    setError('');
    try {
      const res = await api.post('/api/teachers', form);
      onSuccess && onSuccess(res.data);
      onClose();
    } catch (e) {
      setError(e.response?.data || 'Error saving teacher');
    }
  };

  if (!open) return null;
  return (
    <div className="modal teacher-modal">
      <div className="modal-content">
        <h2>{editTeacher ? 'Edit' : 'Add'} Teacher</h2>
        <div className="steps">
          {steps.map((s, i) => <span key={s} className={i === step ? 'active' : ''}>{s}</span>)}
        </div>
        {step === 0 && (
          <div>
            <input placeholder="First Name" value={form.teacher.firstName} onChange={e => handleChange('teacher', 'firstName', e.target.value)} />
            <input placeholder="Last Name" value={form.teacher.lastName} onChange={e => handleChange('teacher', 'lastName', e.target.value)} />
            <input placeholder="Email" value={form.teacher.email} onChange={e => handleChange('teacher', 'email', e.target.value)} />
            <input placeholder="Phone" value={form.teacher.phone} onChange={e => handleChange('teacher', 'phone', e.target.value)} />
            <input placeholder="Employee ID" value={form.teacher.employeeId} onChange={e => handleChange('teacher', 'employeeId', e.target.value)} />
          </div>
        )}
        {step === 1 && (
          <div>
            <input placeholder="Qualification" value={form.teacher.qualification} onChange={e => handleChange('teacher', 'qualification', e.target.value)} />
            <input placeholder="Department" value={form.teacher.department} onChange={e => handleChange('teacher', 'department', e.target.value)} />
            <input placeholder="Specialization" value={form.teacher.specialization} onChange={e => handleChange('teacher', 'specialization', e.target.value)} />
            <input placeholder="Experience (years)" value={form.teacher.experienceYears} onChange={e => handleChange('teacher', 'experienceYears', e.target.value)} />
            <input placeholder="Designation" value={form.teacher.designation} onChange={e => handleChange('teacher', 'designation', e.target.value)} />
          </div>
        )}
        {step === 2 && (
          <div>
            {/* Assignment UI: Multi-select classes, batches, subject, class teacher toggle */}
            <button onClick={() => handleAssignmentAdd({ classId: 1, batchId: 1, subject: 'Math', isClassTeacher: false })}>+ Add Assignment (demo)</button>
            <ul>
              {form.assignments.map((a, i) => <li key={i}>{a.subject} (Class {a.classId}, Batch {a.batchId}) {a.isClassTeacher && '🌟'}</li>)}
            </ul>
          </div>
        )}
        {step === 3 && (
          <div>
            <input placeholder="Username" value={form.credentials.username} onChange={e => handleChange('credentials', 'username', e.target.value)} />
            <input placeholder="Password" type="password" value={form.credentials.password} onChange={e => handleChange('credentials', 'password', e.target.value)} />
            <label><input type="checkbox" checked={form.credentials.passwordResetRequired} onChange={e => handleChange('credentials', 'passwordResetRequired', e.target.checked)} /> Force password reset</label>
          </div>
        )}
        {error && <div className="error">{error}</div>}
        <div className="modal-actions">
          {step > 0 && <button onClick={handlePrev}>Back</button>}
          {step < steps.length - 1 && <button onClick={handleNext}>Next</button>}
          {step === steps.length - 1 && <button onClick={handleSubmit}>{editTeacher ? 'Update' : 'Create'}</button>}
          <button onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  );
}
