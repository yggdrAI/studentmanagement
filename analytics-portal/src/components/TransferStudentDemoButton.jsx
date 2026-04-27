import React, { useState } from "react";
import TransferStudentModal from "./TransferStudentModal";

export default function TransferStudentDemoButton({ studentId, onTransfer }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button onClick={() => setOpen(true)}>
        Transfer Student
      </button>
      <TransferStudentModal
        open={open}
        onClose={() => setOpen(false)}
        studentId={studentId}
        onTransfer={onTransfer}
      />
    </>
  );
}
