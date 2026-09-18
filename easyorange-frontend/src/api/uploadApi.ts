import { request } from './core/request';

/** 与后端 UploadFileVO 对齐；fileSize 是 Long，经 JacksonConfig 序列化为字符串（避免 JS 精度丢失） */
export interface UploadResponse {
    id: string;
    fileName: string;
    fileUrl: string;
    fileSize: string;
    mimeType: string;
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
