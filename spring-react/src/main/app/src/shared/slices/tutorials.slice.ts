import { createAsyncThunk, createSlice, SerializedError } from '@reduxjs/toolkit';
import { DataService } from '../services/data.service.ts';
import { TutorialType } from '../types/tutorial.type.ts';

export interface TutorialsState {
    loading: boolean;
    tutorials: TutorialType[];
    error?: SerializedError;
}

export interface TutorialInfo {
    title: string;
    description: string;
}

const initialState: TutorialsState = {
    loading: false,
    tutorials: [],
    error: undefined
};

const dataService = new DataService();

export const getTutorials = createAsyncThunk('tutorials/getTutorials', async () => {
    return await dataService.getTutorials();
});

export const addTutorials = createAsyncThunk('tutorials/addTutorials', async (info: TutorialInfo) => {
    return await dataService.postTutorial(info.title, info.description);
});

export const tutorialsSlice = createSlice({
    name: 'tutorials',
    initialState,
    reducers: {
        updateTutorial: (state, action) => {
            state.tutorials = action.payload;
        },
    },
    extraReducers: (builder) => {
        builder.addCase(getTutorials.pending, (state) => {
            state.loading = true;
        });
        builder.addCase(getTutorials.fulfilled, (state, action) => {
            state.loading = false;
            state.tutorials = action.payload;
        });
        builder.addCase(getTutorials.rejected, (state, action) => {
            state.loading = false;
            state.tutorials = [];
            state.error = action.error;
        });
        builder.addCase(addTutorials.pending, (state) => {
            state.loading = true;
        });
        builder.addCase(addTutorials.fulfilled, (state) => {
            state.loading = false;
        });
        builder.addCase(addTutorials.rejected, (state, action) => {
            state.loading = false;
            state.tutorials = [];
            state.error = action.error;
        });
    }
});

export const {updateTutorial} = tutorialsSlice.actions;
export default tutorialsSlice.reducer;
