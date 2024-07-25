import { StyledEngineProvider } from '@mui/material';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import App from './App.tsx';
import { store } from './store.ts';

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <Provider store={store}>
            <StyledEngineProvider injectFirst>
                <BrowserRouter>
                    <App/>
                </BrowserRouter>
            </StyledEngineProvider>
        </Provider>
    </React.StrictMode>
);
