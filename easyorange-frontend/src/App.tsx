import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { ErrorBoundary } from './components/feedback/ErrorBoundary';
import motionController from './lib/motion';
import { router } from './routes';

function App() {
    useEffect(() => {
        motionController.init();
        return () => motionController.destroy();
    }, []);

    return (
        <ErrorBoundary>
            <RouterProvider router={router} />
        </ErrorBoundary>
    );
}

export default App;
