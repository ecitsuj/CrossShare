package com.justice.crossshare.network

object WebPageContent {

    fun getHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CrossShare - Local File Sharing</title>
    <style>
        :root {
            --primary: #6366f1;
            --primary-hover: #4f46e5;
            --bg: #0f172a;
            --surface: #1e293b;
            --surface-card: #334155;
            --text-main: #f8fafc;
            --text-sub: #94a3b8;
            --border: #475569;
            --accent-green: #10b981;
            --accent-blue: #3b82f6;
            --radius: 16px;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            background-color: var(--bg);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 24px 16px;
        }

        .container {
            width: 100%;
            max-width: 768px;
            display: flex;
            flex-direction: column;
            gap: 24px;
        }

        header {
            background: linear-gradient(135deg, #312e81, #1e1b4b);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 24px;
            border-radius: var(--radius);
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3);
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .logo-icon {
            width: 48px;
            height: 48px;
            background: linear-gradient(135deg, var(--primary), #818cf8);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            font-weight: bold;
            color: white;
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
        }

        .brand-text h1 {
            font-size: 22px;
            font-weight: 700;
            letter-spacing: -0.5px;
        }

        .brand-text p {
            font-size: 13px;
            color: var(--text-sub);
        }

        .status-badge {
            background: rgba(16, 185, 129, 0.15);
            color: var(--accent-green);
            border: 1px solid rgba(16, 185, 129, 0.3);
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            background-color: var(--accent-green);
            border-radius: 50%;
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
        }

        .card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 24px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
        }

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 16px;
        }

        .card-title {
            font-size: 18px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .dropzone {
            border: 2px dashed var(--border);
            border-radius: 12px;
            padding: 36px 20px;
            text-align: center;
            background: rgba(15, 23, 42, 0.4);
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .dropzone:hover, .dropzone.dragover {
            border-color: var(--primary);
            background: rgba(99, 102, 241, 0.08);
        }

        .dropzone-icon {
            font-size: 40px;
            margin-bottom: 12px;
            display: block;
        }

        .btn {
            background: var(--primary);
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 8px;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            transition: background 0.2s;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            text-decoration: none;
        }

        .btn:hover {
            background: var(--primary-hover);
        }

        .btn-secondary {
            background: var(--surface-card);
            color: var(--text-main);
        }

        .btn-secondary:hover {
            background: #475569;
        }

        .file-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
            margin-top: 12px;
        }

        .file-item {
            background: var(--surface-card);
            border-radius: 10px;
            padding: 12px 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
        }

        .file-info {
            display: flex;
            align-items: center;
            gap: 12px;
            overflow: hidden;
        }

        .file-icon {
            font-size: 24px;
            flex-shrink: 0;
        }

        .file-details {
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .file-name {
            font-size: 14px;
            font-weight: 600;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .file-meta {
            font-size: 12px;
            color: var(--text-sub);
        }

        .progress-container {
            margin-top: 16px;
            display: none;
        }

        .progress-bar-bg {
            background: var(--surface-card);
            border-radius: 8px;
            height: 10px;
            overflow: hidden;
            margin-top: 8px;
        }

        .progress-bar-fill {
            background: linear-gradient(90deg, var(--primary), var(--accent-blue));
            height: 100%;
            width: 0%;
            transition: width 0.1s ease;
        }

        .empty-state {
            text-align: center;
            padding: 24px;
            color: var(--text-sub);
            font-size: 14px;
        }

        footer {
            margin-top: auto;
            text-align: center;
            font-size: 12px;
            color: var(--text-sub);
            padding-top: 24px;
        }
    </style>
</head>
<body>

<div class="container">
    <header>
        <div class="brand">
            <div class="logo-icon">⇄</div>
            <div class="brand-text">
                <h1>CrossShare</h1>
                <p>Seamless High-Speed Local File Sharing</p>
            </div>
        </div>
        <div class="status-badge">
            <div class="status-dot"></div>
            Device Connected
        </div>
    </header>

    <!-- Upload Section -->
    <div class="card">
        <div class="card-header">
            <div class="card-title">
                <span>📤</span> Send Files to Android Device
            </div>
        </div>
        <div class="dropzone" id="dropzone" onclick="document.getElementById('fileInput').click()">
            <span class="dropzone-icon">📁</span>
            <p style="font-weight: 600; margin-bottom: 4px;">Click to browse or drag & drop files here</p>
            <p style="font-size: 12px; color: var(--text-sub);">Supports documents, photos, videos, audio, and archives</p>
            <input type="file" id="fileInput" multiple style="display: none;" onchange="handleFileSelect(this.files)">
        </div>

        <div id="selectedFiles" class="file-list" style="display: none; margin-top: 16px;"></div>

        <div id="progressSection" class="progress-container">
            <div style="display: flex; justify-content: space-between; font-size: 13px;">
                <span id="progressStatus">Uploading...</span>
                <span id="progressPercent" style="font-weight: 600;">0%</span>
            </div>
            <div class="progress-bar-bg">
                <div class="progress-bar-fill" id="progressBar"></div>
            </div>
        </div>
    </div>

    <!-- Download Section -->
    <div class="card">
        <div class="card-header">
            <div class="card-title">
                <span>📥</span> Files Available on Device
            </div>
            <button class="btn btn-secondary" onclick="fetchSharedFiles()" style="padding: 6px 12px; font-size: 12px;">
                🔄 Refresh
            </button>
        </div>
        <div id="sharedFilesList" class="file-list">
            <div class="empty-state">Loading shared files...</div>
        </div>
    </div>
</div>

<footer>
    CrossShare Local Engine • Direct High-Speed Transfer
</footer>

<script>
    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('fileInput');
    const selectedFilesContainer = document.getElementById('selectedFiles');
    const progressSection = document.getElementById('progressSection');
    const progressBar = document.getElementById('progressBar');
    const progressPercent = document.getElementById('progressPercent');
    const progressStatus = document.getElementById('progressStatus');
    const sharedFilesList = document.getElementById('sharedFilesList');

    // Drag and Drop listeners
    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.add('dragover');
        }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.remove('dragover');
        }, false);
    });

    dropzone.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files && files.length > 0) {
            uploadFiles(files);
        }
    });

    function handleFileSelect(files) {
        if (files && files.length > 0) {
            uploadFiles(files);
        }
    }

    function uploadFiles(files) {
        if (!files || files.length === 0) return;

        const formData = new FormData();
        for (let i = 0; i < files.length; i++) {
            formData.append('file', files[i]);
        }

        progressSection.style.display = 'block';
        progressBar.style.width = '0%';
        progressPercent.innerText = '0%';
        progressStatus.innerText = 'Uploading ' + files.length + ' file(s)...';

        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/upload', true);

        xhr.upload.onprogress = function (e) {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                progressBar.style.width = percent + '%';
                progressPercent.innerText = percent + '%';
                progressStatus.innerText = 'Uploaded ' + formatBytes(e.loaded) + ' of ' + formatBytes(e.total);
            }
        };

        xhr.onload = function () {
            if (xhr.status >= 200 && xhr.status < 300) {
                progressBar.style.width = '100%';
                progressPercent.innerText = '100%';
                progressStatus.innerText = 'Upload completed successfully!';
                setTimeout(() => {
                    progressSection.style.display = 'none';
                    fetchSharedFiles();
                }, 2000);
            } else {
                progressStatus.innerText = 'Upload failed! Server returned status ' + xhr.status;
            }
        };

        xhr.onerror = function () {
            progressStatus.innerText = 'Network error occurred during upload.';
        };

        xhr.send(formData);
    }

    function fetchSharedFiles() {
        fetch('/api/files')
            .then(res => res.json())
            .then(files => {
                if (!files || files.length === 0) {
                    sharedFilesList.innerHTML = '<div class="empty-state">No files currently shared from device.</div>';
                    return;
                }
                let html = '';
                files.forEach(file => {
                    const icon = getFileIcon(file.name, file.mimeType);
                    html += `
                        <div class="file-item">
                            <div class="file-info">
                                <span class="file-icon">${'$'}{icon}</span>
                                <div class="file-details">
                                    <span class="file-name" title="${'$'}{escapeHtml(file.name)}">${'$'}{escapeHtml(file.name)}</span>
                                    <span class="file-meta">${'$'}{file.formattedSize || formatBytes(file.size)}</span>
                                </div>
                            </div>
                            <a href="/download/${'$'}{file.id}" class="btn" download="${'$'}{escapeHtml(file.name)}">
                                ⬇ Download
                            </a>
                        </div>
                    `;
                });
                sharedFilesList.innerHTML = html;
            })
            .catch(err => {
                sharedFilesList.innerHTML = '<div class="empty-state">Unable to load shared files.</div>';
            });
    }

    function getFileIcon(filename, mimeType) {
        const ext = filename.split('.').pop().toLowerCase();
        if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) return '🖼️';
        if (['mp4', 'mkv', 'avi', 'mov', 'webm'].includes(ext)) return '🎬';
        if (['mp3', 'wav', 'flac', 'm4a', 'aac'].includes(ext)) return '🎵';
        if (['pdf', 'doc', 'docx', 'txt', 'rtf', 'epub'].includes(ext)) return '📄';
        if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return '📦';
        if (['apk'].includes(ext)) return '🤖';
        return '📁';
    }

    function formatBytes(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }

    // Initial load
    fetchSharedFiles();
</script>
</body>
</html>
        """.trimIndent()
    }
}
