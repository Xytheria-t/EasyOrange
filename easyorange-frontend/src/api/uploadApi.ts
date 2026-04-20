/**
 * @fileoverview 上传 API 模块
 */

const API_BASE_URL = '/api';

export const uploadFile = async (file: File): Promise<{ code: number; message: string; data: { url: string; filename: string; size: number; type: string } }> => {
    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem('token');
    const headers: Record<string, string> = {};
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}/file/upload`, {
        method: 'POST',
        body: formData,
        headers
    });

    return response.json();
};

export const uploadFiles = async (files: File[]): Promise<{ code: number; message: string; data: Array<{ url: string; filename: string; size: number; type: string }> }> => {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));

    const token = localStorage.getItem('token');
    const headers: Record<string, string> = {};
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}/file/uploads`, {
        method: 'POST',
        body: formData,
        headers
    });

    return response.json();
};

export const uploadApi = {
    uploadFile,
    uploadFiles
};
