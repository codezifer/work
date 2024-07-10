import {createAsyncThunk, createSlice, SerializedError} from '@reduxjs/toolkit';
import {TutorialType} from '../types/tutorial.type.ts';
import {DataService} from '../services/data.service.ts';

export interface TutorialsState {
    loading: boolean;
    tutorials: TutorialType[];
    error?: SerializedError;
}

const initialState: TutorialsState = {
    loading: false,
    tutorials: [],
    error: undefined
};

const dataService = new DataService();

export const getTutorials = createAsyncThunk('tutorials/get', () => {
    return dataService.getTutorials()
});

export const tutorialsSlice = createSlice({
    name: 'tutorials',
    initialState,
    reducers: {
        updateTutorial: (state, action) => {
            state.tutorials = action.payload
        }
    },
    extraReducers: (builder) => {
        builder.addCase(getTutorials.pending, (state) => {
            state.loading = true
        });
        builder.addCase(getTutorials.fulfilled, (state, action) => {
            state.loading = false
            state.tutorials = action.payload
        });
        builder.addCase(getTutorials.rejected, (state, action) => {
           state.loading = false
           state.tutorials = []
           state.error = action.error
        });
    }
});

export const { updateTutorial } = tutorialsSlice.actions
export default tutorialsSlice.reducer;