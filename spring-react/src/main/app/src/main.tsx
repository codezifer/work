import {StyledEngineProvider} from '@mui/material';
import React from 'react';
import ReactDOM from 'react-dom/client';
import {BrowserRouter} from 'react-router-dom';
import App from './App.tsx';

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <StyledEngineProvider injectFirst>
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </StyledEngineProvider>
    </React.StrictMode>,
);
