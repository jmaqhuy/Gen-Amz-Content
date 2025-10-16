const form = document.getElementById('contentForm');
const imageInput = document.getElementById('productImage');
const imagePreview = document.getElementById('imagePreview');
const imagePreviewContainer = document.getElementById('imagePreviewContainer');
const placeholderText = document.getElementById('placeholderText');
const loading = document.getElementById('loading');
const buttonSpinner = document.getElementById('buttonSpinner');
const buttonText = document.getElementById('buttonText');
const dragOverlay = document.getElementById('dragOverlay');

// Tự động khôi phục thông tin mẫu khi mở lại trang
document.addEventListener('DOMContentLoaded', () => {
    const fields = ['material', 'dimensions', 'tags']; // id trong HTML
    try {
        fields.forEach(f => {
            const el = document.getElementById(f);
            const val = localStorage.getItem(f);
            if (el && val) el.value = val;
        });
    } catch (err) {
        console.warn("⚠️ Không thể load dữ liệu mẫu:", err);
    }
});

// Click to select file
imagePreviewContainer.addEventListener('click', () => {
    imageInput.click();
});

// Image preview
imageInput.addEventListener('change', function(e) {
    handleImageSelect(e.target.files[0]);
});

// Drag and drop
document.addEventListener('dragover', (e) => {
    e.preventDefault();
    dragOverlay.classList.add('show');
});

document.addEventListener('dragleave', (e) => {
    if (e.target === document) {
        dragOverlay.classList.remove('show');
    }
});

document.addEventListener('drop', (e) => {
    e.preventDefault();
    dragOverlay.classList.remove('show');

    const files = e.dataTransfer.files;
    const imageFile = Array.from(files).find(file => file.type.startsWith('image/'));

    if (imageFile) {
        handleImageSelect(imageFile);
        imageInput.files = files;
    }
});

function handleImageSelect(file) {
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            imagePreview.src = e.target.result;
            imagePreview.classList.add('show');
            placeholderText.style.display = 'none';
        };
        reader.readAsDataURL(file);
    }
}

// Form submission
form.addEventListener('submit', async function(e) {
    e.preventDefault();

    const formData = new FormData(form);
    try {
        const material = document.getElementById('material')?.value || '';
        const dimensions = document.getElementById('dimensions')?.value || '';
        const tags = document.getElementById('tags')?.value || '';

        const sampleData = { material, dimensions, tags };

        Object.entries(sampleData).forEach(([key, val]) => {
            localStorage.setItem(key, val);
        });

        console.log("✅ Đã lưu thông tin mẫu:", sampleData);
    } catch (err) {
        console.warn("⚠️ Không thể lưu thông tin mẫu:", err);
    }

    loading.classList.add('show');
    buttonSpinner.style.display = 'inline-block';
    buttonText.textContent = 'Đang tạo...';

    try {
        const response = await fetch('/api/generate-content', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (response.ok) {
            console.log(data);
            populateOutputs(data);
        } else {
            alert('Lỗi: ' + (data.message || 'Không thể tạo nội dung'));
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
        loading.classList.remove('show');
        buttonSpinner.style.display = 'none';
        buttonText.textContent = 'Tạo nội dung';
    }
});

function populateOutputs(data) {
    updateOutput('title', data.title);
    updateOutput('description', data.description);
    updateOutput('bulletPoints', formatBulletPoints(data.bullet_points));
    updateOutput('tags', data.tags);
    updateOutput('slugs', formatBulletPoints(data.slugs));
}

function formatBulletPoints(bulletPoints) {
    if (Array.isArray(bulletPoints)) {
        return bulletPoints.join('\n');
    }
    return bulletPoints;
}

function updateOutput(id, content) {
    const element = document.getElementById(id + 'Output');
    if (content) {
        element.textContent = content;
        element.classList.remove('empty');
    } else {
        element.textContent = 'Không có nội dung';
        element.classList.add('empty');
    }
}

function copyOutput(type) {
    const element = document.getElementById(type + 'Output');
    let text;
    if (type === 'slugs') {
        text = '"' + element.textContent.trim().replace(/"/g, '""') + '"';
    } else {
        text = element.textContent;
    }
    navigator.clipboard.writeText(text).then(() => {
        showFeedback(type + 'Feedback');
    });
}

function copyAllForSheet() {
    const title = document.getElementById('titleOutput').textContent;
    const description = document.getElementById('descriptionOutput').textContent;
    const bulletPoints = document.getElementById('bulletPointsOutput').textContent;
    const tags = document.getElementById('tagsOutput').textContent;
    const slugs = document.getElementById('slugsOutput').textContent;

    // Split bullet points by newline for separate columns
    const bulletArray = bulletPoints.split('\n').filter(b => b.trim());
    const slugsCell = '"' + slugs.trim().replace(/"/g, '""') + '"';
    // Format for Google Sheet (tab-separated for columns)
    const sheetText = [title, description, ...bulletArray, tags, slugsCell].join('\t');

    navigator.clipboard.writeText(sheetText).then(() => {
        showFeedback('copyAllFeedback');
    });
}

function showFeedback(id) {
    const feedback = document.getElementById(id);
    feedback.classList.add('show');
    setTimeout(() => {
        feedback.classList.remove('show');
    }, 2000);
}