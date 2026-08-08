import { request } from './core/request';

interface UploadResponse {
    url: string;
    filename: string;
    size: number;
    type: string;
}

export const uploadFile = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);

    return request<UploadResponse>('/file/upload', {
        method: 'POST',
        body: formData,
        headers: {},
        dedupe: false,
    });
};

export const uploadFiles = async (files: File[]) => {
    const formData = new FormData();
    files.forEach(file => {
        formData.append('files', file);
    });

    return request<UploadResponse[]>('/file/uploads', {
        method: 'POST',
        body: formData,
        headers: {},
        dedupe: false,
    });
};

export const uploadApi = {
    uploadFile,
    uploadFiles,
};
