import { StyledEngineProvider } from '@mui/material';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import App from './App.tsx';
import { DataService } from './shared/services/data.service.ts';
import { store } from './store.ts';

const dataService = new DataService();
ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <Provider store={store}>
            <StyledEngineProvider injectFirst>
                <BrowserRouter>
                    <App dataService={dataService}/>
                </BrowserRouter>
            </StyledEngineProvider>
        </Provider>
    </React.StrictMode>
);
