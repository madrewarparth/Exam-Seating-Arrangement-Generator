document.addEventListener('DOMContentLoaded', () => {
    const csvFileInput = document.getElementById('csvFile');
    const fileNameSpan = document.getElementById('fileName');
    const uploadBtn = document.getElementById('uploadBtn');
    const uploadStatus = document.getElementById('uploadStatus');
    
    const examDateInput = document.getElementById('examDate');
    const generateBtn = document.getElementById('generateBtn');
    const exportBtn = document.getElementById('exportBtn');
    
    const resultsSection = document.getElementById('resultsSection');
    const planPreview = document.getElementById('planPreview');

    // Set today's date as default
    const today = new Date().toISOString().split('T')[0];
    examDateInput.value = today;

    // File selection
    csvFileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            fileNameSpan.textContent = e.target.files[0].name;
            uploadBtn.disabled = false;
        } else {
            fileNameSpan.textContent = 'No file selected';
            uploadBtn.disabled = true;
        }
    });

    // Upload CSV
    uploadBtn.addEventListener('click', async () => {
        const file = csvFileInput.files[0];
        if (!file) return;

        const formData = new FormData();
        formData.append('file', file);

        try {
            uploadStatus.textContent = 'Uploading...';
            uploadStatus.className = 'status-msg';
            
            const response = await fetch('/api/upload', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                uploadStatus.textContent = await response.text();
                uploadStatus.className = 'status-msg success';
                generateBtn.disabled = false;
            } else {
                uploadStatus.textContent = await response.text();
                uploadStatus.className = 'status-msg error';
            }
        } catch (error) {
            uploadStatus.textContent = 'Error connecting to server.';
            uploadStatus.className = 'status-msg error';
        }
    });

    // Generate Plan
    generateBtn.addEventListener('click', async () => {
        const date = examDateInput.value;
        try {
            const response = await fetch(`/api/generate?examDate=${date}`);
            
            if (response.ok) {
                const plan = await response.json();
                displayPlan(plan);
                exportBtn.style.display = 'inline-block';
            } else {
                alert('Failed to generate plan. Please ensure you uploaded a CSV.');
            }
        } catch (error) {
            alert('Error connecting to server.');
        }
    });

    // Export PDF
    exportBtn.addEventListener('click', () => {
        const date = examDateInput.value;
        window.open(`/api/export/pdf?examDate=${date}`, '_blank');
    });

    function displayPlan(plan) {
        resultsSection.style.display = 'block';
        planPreview.innerHTML = '';

        if (!plan || plan.length === 0) {
            planPreview.innerHTML = '<p>No seating plan generated.</p>';
            return;
        }

        // Group by room
        const rooms = {};
        plan.forEach(item => {
            if (!rooms[item.roomNumber]) {
                rooms[item.roomNumber] = [];
            }
            rooms[item.roomNumber].push(item);
        });

        // Create tables
        for (const [room, students] of Object.entries(rooms)) {
            const section = document.createElement('div');
            section.className = 'room-section';
            
            const title = document.createElement('h3');
            title.textContent = `Room: ${room}`;
            section.appendChild(title);

            const table = document.createElement('table');
            table.innerHTML = `
                <thead>
                    <tr>
                        <th>Bench</th>
                        <th>Roll No</th>
                        <th>Name</th>
                        <th>Branch</th>
                    </tr>
                </thead>
                <tbody>
                    ${students.map(s => `
                        <tr>
                            <td>${s.benchNumber}</td>
                            <td>${s.student.rollNo}</td>
                            <td>${s.student.name}</td>
                            <td>${s.student.branch}</td>
                        </tr>
                    `).join('')}
                </tbody>
            `;
            section.appendChild(table);
            planPreview.appendChild(section);
        }
    }
});
