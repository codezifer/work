import { Box, Snackbar, TextField } from '@mui/material';
import Button from '@mui/material/Button';
import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { addTutorials } from '../shared/slices/tutorials.slice.ts';
import { AppDispatch } from '../store.ts';

export const TutorialAdd = () => {

    const EMPTY_VALUE = '---';

    const [title, setTitle] = useState(EMPTY_VALUE);
    const [description, setDescription] = useState(EMPTY_VALUE);
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState(EMPTY_VALUE);
    const dispatch = useDispatch<AppDispatch>();

    const onSubmit = (target: string) => {
        console.log(`... on submit ${target} ...`);
    };

    const onTitleChange = (title?: string) => {
        setTitle(title ?? EMPTY_VALUE);
    };

    const onDescriptionChange = (desc?: string) => {
        setDescription(desc ?? EMPTY_VALUE);
    };

    const checkValue = (value?: string) => {
        return !!value && value.length > 0 && value !== EMPTY_VALUE;
    };

    const onPublish = () => {
        if (checkValue(title) && checkValue(description)) {
            dispatch(addTutorials({title, description})).then(() => {
                setIsOpen(true);
                setMessage(`Tutorial ${title} published!`);
            });
        } else {
            console.warn('... no title or description ...');
        }
    };

    const onClose = () => {
        setIsOpen(prevState => !prevState);
    };

    const padding = '8px';
    return (
        <Box sx={{
            minWidth: 120,
            margin: padding
        }}>
            <form autoComplete="off" onSubmit={e => onSubmit(e.currentTarget.target)}>
                <h4>Add Tutorial</h4>
                <TextField
                    fullWidth
                    type="text"
                    label="Title"
                    margin="dense"
                    component="form"
                    onChange={e => onTitleChange(e.target.value)}
                />

                <TextField
                    fullWidth
                    type="text"
                    label="Description"
                    margin="dense"
                    component="form"
                    onChange={e => onDescriptionChange(e.target.value)}
                />
                <Button variant="contained" sx={{margin: padding}} onClick={onPublish}>PUBLISH</Button>
                <Snackbar
                    open={isOpen}
                    autoHideDuration={5000}
                    onClose={onClose}
                    message={message}
                />
            </form>
        </Box>
    );
};
