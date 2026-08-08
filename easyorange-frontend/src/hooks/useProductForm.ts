import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { uploadFile } from '@/api/uploadApi';
import { type PublishFormData, publishSchema } from '@/schemas/publishSchema';
import { useUIStore } from '@/store/uiStore';
import { compressImage } from '@/utils/imageCompress';

export const PRODUCT_FORM_DEFAULTS: PublishFormData = {
    name: '',
    description: '',
    price: '',
    originalPrice: '',
    categoryId: '',
    conditionLevel: '',
    stock: '1',
    location: '',
    contactMethod: '',
    imageUrls: [],
};

export interface ProductPayload {
    name: string;
    description: string;
    price: number;
    originalPrice?: number;
    categoryId: string;
    conditionLevel: number;
    stock: number;
    location?: string;
    contactMethod?: string;
    imageUrls: string[];
}

export function buildProductPayload(data: PublishFormData): ProductPayload {
    return {
        name: data.name.trim(),
        description: data.description.trim(),
        price: Number(data.price),
        originalPrice: data.originalPrice ? Number(data.originalPrice) : undefined,
        categoryId: data.categoryId,
        conditionLevel: Number(data.conditionLevel),
        stock: Number(data.stock) || 1,
        location: data.location.trim() || undefined,
        contactMethod: data.contactMethod.trim() || undefined,
        imageUrls: data.imageUrls,
    };
}

/**
 * 发布/编辑商品共用表单装配：useForm 配置 + 图片压缩上传 + 图片删除。
 */
export function useProductForm() {
    const addToast = useUIStore(s => s.addToast);
    const [uploadingIndex, setUploadingIndex] = useState<number | null>(null);
    const form = useForm<PublishFormData>({
        resolver: zodResolver(publishSchema),
        reValidateMode: 'onChange',
        defaultValues: PRODUCT_FORM_DEFAULTS,
    });
    const { watch, setValue } = form;

    const processFiles = async (files: File[]) => {
        for (const file of files) {
            const currentImages = watch('imageUrls');
            if (currentImages.length >= 9) {
                break;
            }
            if (!file.type.startsWith('image/')) {
                continue;
            }
            if (file.size > 10 * 1024 * 1024) {
                continue;
            }

            setUploadingIndex(currentImages.length);
            try {
                const compressed = await compressImage(file);
                const result = await uploadFile(compressed);
                if (result.data?.url) {
                    setValue('imageUrls', [...watch('imageUrls'), result.data.url], { shouldValidate: true });
                }
            } catch {
                addToast({ type: 'error', message: '图片上传失败，请重试' });
            } finally {
                setUploadingIndex(null);
            }
        }
    };

    const handleImageSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files) {
            return;
        }
        await processFiles(Array.from(files));
        e.target.value = '';
    };

    const handleImageUrlRemove = (index: number) => {
        setValue(
            'imageUrls',
            watch('imageUrls').filter((_, i) => i !== index),
            { shouldValidate: true }
        );
    };

    return { ...form, processFiles, handleImageSelect, handleImageUrlRemove, uploadingIndex };
}
