import { KeyRound, Lock } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
} from '@/components/ui';
import { Button } from '@/components/ui/button';
import { type ChangePasswordForm, changePasswordSchema } from '@/schemas/authSchema';

interface PasswordModalProps {
    show: boolean;
    isLoading: boolean;
    onClose: () => void;
    onSubmit: (data: ChangePasswordForm) => void;
}

export function PasswordModal({ show, isLoading, onClose, onSubmit }: PasswordModalProps) {
    const {
        register,
        handleSubmit,
        formState: { errors },
        reset,
    } = useForm<ChangePasswordForm>({
        resolver: zodResolver(changePasswordSchema),
        reValidateMode: 'onChange',
        defaultValues: { oldPassword: '', newPassword: '', confirmPassword: '' },
    });

    const handleClose = () => {
        reset();
        onClose();
    };

    return (
        <Dialog open={show} onOpenChange={open => !open && handleClose()}>
            <DialogContent className="gap-0 p-0 overflow-hidden rounded-3xl bg-white/94 backdrop-blur-2xl border-border/60 sm:max-w-[480px]">
                <DialogHeader className="px-6 pt-6 pb-2">
                    <DialogTitle className="text-[1.25rem] font-bold tracking-tight">修改密码</DialogTitle>
                </DialogHeader>

                <form onSubmit={handleSubmit(data => onSubmit(data))} className="p-6 space-y-5">
                    <div className="space-y-2">
                        <Label htmlFor="old-password">旧密码</Label>
                        <div className="relative">
                            <Lock
                                size={18}
                                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
                            />
                            <Input
                                id="old-password"
                                type="password"
                                placeholder="请输入旧密码"
                                className="pl-10"
                                {...register('oldPassword')}
                            />
                        </div>
                        {errors.oldPassword && (
                            <p className="text-sm text-destructive mt-1">{errors.oldPassword.message}</p>
                        )}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="new-password">新密码</Label>
                        <div className="relative">
                            <KeyRound
                                size={18}
                                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
                            />
                            <Input
                                id="new-password"
                                type="password"
                                placeholder="至少8位字符"
                                className="pl-10"
                                {...register('newPassword')}
                            />
                        </div>
                        {errors.newPassword && (
                            <p className="text-sm text-destructive mt-1">{errors.newPassword.message}</p>
                        )}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="confirm-password">确认新密码</Label>
                        <div className="relative">
                            <KeyRound
                                size={18}
                                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
                            />
                            <Input
                                id="confirm-password"
                                type="password"
                                placeholder="再次输入新密码"
                                className="pl-10"
                                {...register('confirmPassword')}
                            />
                        </div>
                        {errors.confirmPassword && (
                            <p className="text-sm text-destructive mt-1">{errors.confirmPassword.message}</p>
                        )}
                    </div>

                    <DialogFooter className="px-0 pt-2 border-t border-border/40 gap-2">
                        <Button variant="outline" onClick={handleClose} className="rounded-xl px-5" type="button">
                            取消
                        </Button>
                        <Button
                            type="submit"
                            disabled={isLoading}
                            isLoading={isLoading}
                            loadingText="修改中..."
                            className="rounded-xl px-6"
                        >
                            确认修改
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
