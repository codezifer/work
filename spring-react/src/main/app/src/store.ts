import { configureStore } from '@reduxjs/toolkit';
import tutorialsReducer from './shared/slices/tutorials.slice';

export const store = configureStore({
    reducer: {
        tutorials: tutorialsReducer
    },
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware({
            serializableCheck: {
                ignoredActionPaths: ['payload.headers']
            }
        }),
    devTools: true
});

export type AppDispatch = typeof store.dispatch;
export type RootState = ReturnType<typeof store.getState>
