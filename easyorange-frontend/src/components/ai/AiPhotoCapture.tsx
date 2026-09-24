import { Camera, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import './ai-components.css';

interface AiPhotoCaptureProps {
    onAnalyze: () => void;
    isLoading: boolean;
    hasImages: boolean;
}

export function AiPhotoCapture({ onAnalyze, isLoading, hasImages }: AiPhotoCaptureProps) {
    if (!hasImages) {
        return null;
    }

    return (
        <div className="ai-photo-capture">
            <Button className="ai-photo-btn" onClick={onAnalyze} disabled={isLoading}>
                {isLoading ? (
                    <>
                        <Loader2 size={16} className="animate-spin" />
                        正在识别...
                    </>
                ) : (
                    <>
                        <Camera size={16} />
                        AI 智能识别
                    </>
                )}
            </Button>
            {/* 后端 prompt 明确「画面中没有地点信息就返回空串」，商品图推不出地点，
                前端空值保护也不会回填 —— 承诺里不放做不到的字段 */}
            <p className="ai-photo-hint">一键识别商品信息，自动填写名称、描述、价格、类别和成色</p>
        </div>
    );
}
