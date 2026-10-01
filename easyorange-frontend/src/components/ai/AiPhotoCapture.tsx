import { AlertCircle, Camera, Loader2, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import './ai-components.css';

interface AiPhotoCaptureProps {
    onAnalyze: () => void;
    isLoading: boolean;
    hasImages: boolean;
    /** 最近一次识别失败的文案（持久展示 + 重试入口；重新发起或成功后由 hook 清除） */
    failure?: string | null;
}

export function AiPhotoCapture({ onAnalyze, isLoading, hasImages, failure }: AiPhotoCaptureProps) {
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
            {/* 失败只弹 toast 的话，用户转身就忘 —— 持久错误条把「失败了」钉在识别按钮旁，
                重试就是同一动作（onAnalyze），不再多一个回调形态 */}
            {failure && !isLoading && (
                <p className="ai-photo-failure" role="alert">
                    <AlertCircle size={13} aria-hidden="true" />
                    <span>{failure}</span>
                    <button type="button" className="ai-photo-retry" onClick={onAnalyze}>
                        <RefreshCw size={12} aria-hidden="true" />
                        重试
                    </button>
                </p>
            )}
        </div>
    );
}
