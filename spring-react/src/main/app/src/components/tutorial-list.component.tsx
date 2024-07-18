import { Box, FormControl, InputLabel, Select, TextField } from '@mui/material';
import MenuItem from '@mui/material/MenuItem';
import { DataGrid, GridColDef, GridRowParams } from '@mui/x-data-grid';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { getTutorials } from '../shared/slices/tutorials.slice';
import { TutorialType } from '../shared/types/tutorial.type.ts';
import { AppDispatch, RootState } from '../store.ts';

type RowType = {
    id?: number;
    title?: string;
    desc?: string;
    created?: Date;
    modified?: Date;
}


export const TutorialList = () => {
    const DEFAULT = ['---'];
    const EMPTY_VALUE = DEFAULT[0];

    const [tutorials, setTutorials] = useState(DEFAULT);
    const [selectedTitle, setSelectedTitle] = useState('None');
    const [selectedDescription, setSelectedDescription] = useState(EMPTY_VALUE);
    const [rows, setRows] = useState([] as RowType[]);

    const dispatch = useDispatch<AppDispatch>();

    const colDefs: GridColDef[] = [
        {field: 'id', headerName: 'ID'},
        {field: 'title', headerName: 'TITLE'},
        {field: 'desc', headerName: 'DESCRIPTION'},
        {field: 'created', headerName: 'CREATED'},
        {field: 'modified', headerName: 'MODIFIED'}
    ];

    useEffect(() => {
        dispatch(getTutorials());
    }, [dispatch]);

    const currentTutorials: TutorialType[] = useSelector((state: RootState) => state.tutorials.tutorials);

    useEffect(() => {
        const rows = currentTutorials.map(t => {
            return {
                id: t.id,
                title: t.title,
                desc: t.description,
                created: t.created,
                modified: t.modified
            } as RowType;
        });
        setRows(rows);
    }, [currentTutorials]);

    const onSelectOpen = () => {
        console.log('... on select ...');
        setTutorials(DEFAULT.concat(currentTutorials.filter(t => !!t.title).map(t => t.title!)));
    };

    const onSelectChange = (title: string) => {
        const tt = currentTutorials.filter(t => t.title === title).shift();
        setSelectedTitle(tt?.title ?? EMPTY_VALUE);
        setSelectedDescription(tt?.description ?? EMPTY_VALUE);
    };

    const onSubmit = () => {
        console.log('... on submit ...');
    };

    const showTutorials = () => {
        return tutorials.map((title, idx) => {
            return (
                <MenuItem key={`${title}.${idx}`} value={title}>{title.toUpperCase()}</MenuItem>
            );
        });
    };

    const onTitleChange = (title: string) => {
        console.log(`... title changed to ${title} ...`);
        setSelectedTitle(title);
    };

    const onDescChange = (desc: string) => {
        console.log(`... description changed to ${desc} ...`);
        setSelectedDescription(desc);
    };

    const onRowClicked = (params: GridRowParams) => {
        setSelectedTitle(params.row.title);
        setSelectedDescription(params.row.desc);
    };

    const padding = '8px';
    return (
        <Box sx={{
            minWidth: 120,
            margin: padding,
            padding: padding
        }}>
            <FormControl fullWidth>
                <InputLabel id="select-tutorials-label">Tutorials</InputLabel>
                <Select onOpen={onSelectOpen} onChange={e => onSelectChange(e.target.value as string)}>
                    {showTutorials()}
                </Select>
                <form autoComplete="off" onSubmit={onSubmit}>
                    <h4>{selectedTitle === EMPTY_VALUE ? 'NONE' : selectedTitle.toUpperCase()}</h4>
                    <TextField
                        fullWidth
                        type="text"
                        label="Title"
                        margin="dense"
                        component="form"
                        value={selectedTitle || EMPTY_VALUE}
                        onChange={e => onTitleChange(e.target.value)}
                    />

                    <TextField
                        fullWidth
                        type="text"
                        label="Description"
                        margin="dense"
                        component="form"
                        value={selectedDescription || EMPTY_VALUE}
                        onChange={e => onDescChange(e.target.value)}
                    />
                </form>

                <DataGrid
                    columns={colDefs}
                    rows={rows}
                    checkboxSelection
                    disableRowSelectionOnClick
                    onRowClick={onRowClicked}/>
            </FormControl>
        </Box>
    );

};

