import { describe, expect, it } from 'vitest';
import { buildProductPayload, PRODUCT_FORM_DEFAULTS } from './useProductForm';

function payloadWith(imageUrls: string[]) {
    return buildProductPayload({ ...PRODUCT_FORM_DEFAULTS, imageUrls });
}

describe('buildProductPayload', () => {
    it('相对图片路径按当前源绝对化（上传接口返回 /api/file/… 的场景）', () => {
        const payload = payloadWith(['/api/file/2026/09/22/abc.jpg']);

        expect(payload.imageUrls).toEqual([`${location.origin}/api/file/2026/09/22/abc.jpg`]);
    });

    it('已是绝对地址的原样保留（编辑页回填后再次提交的场景）', () => {
        const absolute = 'https://cdn.example.com/a.png';

        expect(payloadWith([absolute]).imageUrls).toEqual([absolute]);
    });
});
