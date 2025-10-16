let selectedFiles = [];
let uploadedUrls = [];
const API_BASE = '/api/images';

const uploadZone = document.getElementById('uploadZone');
const fileInput = document.getElementById('fileInput');
const previewGrid = document.getElementById('previewGrid');
const uploadButton = document.getElementById('uploadButton');
const uploadButtonText = document.getElementById('uploadButtonText');
const resultContent = document.getElementById('resultContent');
const selectedCount = document.getElementById('selectedCount');
const uploadedCount = document.getElementById('uploadedCount');
const copyButton = document.getElementById('copyButton');
const successAlert = document.getElementById('successAlert');
const errorAlert = document.getElementById('errorAlert');

// Click to select files
uploadZone.addEventListener('click', () => fileInput.click());

    // File input change
fileInput.addEventListener('change', (e) => {
    handleFiles(e.target.files);
});

    // Drag and drop
uploadZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.classList.add('drag-over');
});

uploadZone.addEventListener('dragleave', () => {
    uploadZone.classList.remove('drag-over');
});

uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('drag-over');
    handleFiles(e.dataTransfer.files);
});

function handleFiles(files) {
    const fileArray = Array.from(files);

    if (fileArray.length > 8) {
    showAlert('error', 'Chỉ được chọn tối đa 8 ảnh!');
    return;
}

selectedFiles = fileArray.filter(file => file.type.startsWith('image/'));

if (selectedFiles.length === 0) {
    showAlert('error', 'Vui lòng chọn file ảnh hợp lệ!');
    return;
}

updatePreview();
updateStats();
updateUploadButton();
}

    function updatePreview() {
    previewGrid.innerHTML = '';
    selectedFiles.forEach((file, index) => {
    const reader = new FileReader();
    reader.onload = (e) => {
    const item = document.createElement('div');
    item.className = 'preview-item';
    item.innerHTML = `
                        <img src="${e.target.result}" class="preview-image" alt="${file.name}">
                        <button class="preview-remove" onclick="removeFile(${index})">×</button>
                        <div class="preview-name" title="${file.name}">${file.name}</div>
                    `;
    previewGrid.appendChild(item);
};
    reader.readAsDataURL(file);
});
}

    function removeFile(index) {
    selectedFiles.splice(index, 1);
    updatePreview();
    updateStats();
    updateUploadButton();
}

    function updateStats() {
    selectedCount.textContent = selectedFiles.length;
}

    function updateUploadButton() {
    if (selectedFiles.length > 0) {
    uploadButton.disabled = false;
    uploadButtonText.textContent = `Upload ${selectedFiles.length} ảnh`;
} else {
    uploadButton.disabled = true;
    uploadButtonText.textContent = 'Chọn ảnh để upload';
}
}

    uploadButton.addEventListener('click', async () => {
    if (selectedFiles.length === 0) return;

    uploadButton.disabled = true;
    uploadButtonText.innerHTML = '<span class="spinner"></span> Đang upload...';

    const formData = new FormData();
    selectedFiles.forEach(file => {
    formData.append('files', file);
});

    try {
    const response = await fetch(`${API_BASE}/upload`, {
    method: 'POST',
    body: formData
});

    const data = await response.json();

    if (response.ok) {
    uploadedUrls = data.urls;
    displayResults(data.urls);
    showAlert('success', `✓ Upload thành công ${data.count} ảnh!`);
    uploadedCount.textContent = data.count;

    // Clear selection
    selectedFiles = [];
    fileInput.value = '';
    updatePreview();
    updateStats();
    updateUploadButton();
} else {
    showAlert('error', data.error || 'Có lỗi xảy ra khi upload!');
}
} catch (error) {
    showAlert('error', 'Không thể kết nối đến server!');
    console.error(error);
} finally {
    uploadButton.disabled = false;
    uploadButtonText.textContent = 'Chọn ảnh để upload';
}
});

    function displayResults(urls) {
    resultContent.classList.remove('empty');
    resultContent.innerHTML = '<div class="result-urls"></div>';

    const urlsContainer = resultContent.querySelector('.result-urls');
    urls.forEach(url => {
    const urlItem = document.createElement('div');
    urlItem.className = 'url-item';
    urlItem.textContent = url;
    urlsContainer.appendChild(urlItem);
});

    copyButton.disabled = false;
}

    copyButton.addEventListener('click', () => {
    if (uploadedUrls.length === 0) return;

    // Copy as tab-separated values for horizontal paste in Google Sheets
    const textToCopy = uploadedUrls.join('\t');

    navigator.clipboard.writeText(textToCopy).then(() => {
    const originalText = copyButton.innerHTML;
    copyButton.innerHTML = '✓ Đã copy!';
    copyButton.style.background = 'hsl(var(--success))';

    setTimeout(() => {
    copyButton.innerHTML = originalText;
    copyButton.style.background = '';
}, 2000);
}).catch(err => {
    showAlert('error', 'Không thể copy! Vui lòng copy thủ công.');
});
});

    function showAlert(type, message) {
    const alert = type === 'success' ? successAlert : errorAlert;
    alert.textContent = message;
    alert.classList.remove('hidden');

    setTimeout(() => {
    alert.classList.add('hidden');
}, 5000);
}

    // Make removeFile global
    window.removeFile = removeFile;
