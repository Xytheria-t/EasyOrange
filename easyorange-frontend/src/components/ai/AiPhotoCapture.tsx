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
            <p className="ai-photo-hint">一键识别商品信息，自动填写名称、描述和类别</p>
        </div>
    );
}
