(function(){
  const form = document.getElementById('uploadForm');
  const result = document.getElementById('uploadResult');

  form.addEventListener('submit', async function(e){
    e.preventDefault();
    result.textContent = 'Uploading...';

    const type = document.getElementById('type').value;
    const classId = document.getElementById('classId').value;
    const batchId = document.getElementById('batchId').value;
    const file = document.getElementById('file').files[0];
    if (!file) { result.textContent = 'Please choose a CSV file.'; return; }

    const formData = new FormData();
    formData.append('file', file);
    if (classId) formData.append('classId', classId);
    if (batchId) formData.append('batchId', batchId);

    try {
      const resp = await fetch('/api/admin/upload/' + encodeURIComponent(type), {
        method: 'POST',
        body: formData
      });

      if (!resp.ok) {
        const text = await resp.text();
        result.textContent = 'Upload failed: ' + text;
      } else {
        const text = await resp.text();
        result.textContent = 'Success: ' + text;
      }
    } catch (err) {
      result.textContent = 'Upload error: ' + err.message;
    }
  });
})();
