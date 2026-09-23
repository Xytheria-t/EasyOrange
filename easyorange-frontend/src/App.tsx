import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { ErrorBoundary } from './components/feedback/ErrorBoundary';
import { ProfileSetupModal } from './components/profile/ProfileSetupModal';
import motionController from './lib/motion';
import { router } from './routes';
import { useUIStore } from './store/uiStore';

function App() {
    useEffect(() => {
        motionController.init();
        return () => motionController.destroy();
    }, []);

    const profileSetup = useUIStore(s => s.profileSetup);
    const closeProfileSetup = useUIStore(s => s.closeProfileSetup);

    return (
        <ErrorBoundary>
            <RouterProvider router={router} />
            <ProfileSetupModal
                isOpen={profileSetup.open}
                onClose={closeProfileSetup}
                username={profileSetup.username}
            />
        </ErrorBoundary>
    );
}

export default App;
