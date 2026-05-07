import { RouterProvider } from 'react-router-dom';
import { router } from './routes';
import { ErrorBoundary } from './components/ErrorBoundary';
import motionController from './lib/motion';
import { useEffect } from 'react';

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
