import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";

export default function TransferStudentModal({ open, onClose, studentId, onTransfer }) {
  const [hierarchy, setHierarchy] = useState([]);
  const [selectedClass, setSelectedClass] = useState("");
  const [selectedBatch, setSelectedBatch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (open) {
      setLoading(true);
      fetch("/api/students/batches-hierarchy")
        .then((res) => res.json())
        .then((data) => {
          setHierarchy(data);
          setLoading(false);
        })
        .catch(() => {
          setError("Failed to load batch hierarchy");
          setLoading(false);
        });
    }
  }, [open]);

  const handleTransfer = () => {
    setConfirming(false);
    setLoading(true);
    setError("");
    setSuccess("");
    fetch(`/api/students/${studentId}/transfer`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        studentId,
        targetBatchId: selectedBatch,
        targetClassId: selectedClass,
      }),
    })
      .then((res) => res.json())
      .then((data) => {
        setLoading(false);
        if (data.success) {
          setSuccess("Student transferred successfully");
          if (onTransfer) onTransfer();
        } else {
          setError(data.message || "Transfer failed");
        }
      })
      .catch(() => {
        setLoading(false);
        setError("Transfer failed");
      });
  };

  if (!open) return null;

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <h2>Transfer Student</h2>
        {loading && <div>Loading...</div>}
        {error && <div className="error">{error}</div>}
        {success && <div className="success">{success}</div>}
        <button className="close-btn" onClick={onClose}>&times;</button>
        <div>
          <label>Class:</label>
          <select
            value={selectedClass}
            onChange={e => {
              setSelectedClass(e.target.value);
              setSelectedBatch("");
            }}
          >
            <option value="">Select Class</option>
            {hierarchy.map(c => (
              <option key={c.classId} value={c.classId}>{c.className}</option>
            ))}
          </select>
        </div>
        {selectedClass && (
          <div>
            <label>Batch:</label>
            <select
              value={selectedBatch}
              onChange={e => setSelectedBatch(e.target.value)}
            >
              <option value="">Select Batch</option>
              {hierarchy.find(c => c.classId === selectedClass)?.batches.map(b => (
                <option key={b.batchId} value={b.batchId}>{b.batchName}</option>
              ))}
            </select>
          </div>
        )}
        <div style={{ marginTop: 16 }}>
          <button
            disabled={!selectedClass || !selectedBatch || loading}
            onClick={() => setConfirming(true)}
          >
            Transfer
          </button>
        </div>
        {confirming && (
          <div className="modal-confirm">
            <div>Are you sure you want to transfer this student?</div>
            <button onClick={handleTransfer}>Yes, Transfer</button>
            <button onClick={() => setConfirming(false)}>Cancel</button>
          </div>
        )}
      </div>
      <style>{`
        .modal-backdrop {
          position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
          background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000;
        }
        .modal {
          background: #fff; padding: 2rem; border-radius: 8px; min-width: 320px; position: relative;
        }
        .close-btn {
          position: absolute; top: 8px; right: 8px; background: none; border: none; font-size: 1.5rem; cursor: pointer;
        }
        .error { color: #b00; margin: 8px 0; }
        .success { color: #080; margin: 8px 0; }
        .modal-confirm {
          background: #f8f8f8; border: 1px solid #ccc; padding: 1rem; margin-top: 1rem; border-radius: 6px;
        }
      `}</style>
    </div>
  );
}

TransferStudentModal.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  studentId: PropTypes.string.isRequired,
  onTransfer: PropTypes.func,
};
