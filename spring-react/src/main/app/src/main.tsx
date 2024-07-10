import {Grid} from '@mui/material';
import React from 'react';
import ReactDOM from 'react-dom/client';
import {createBrowserRouter, RouterProvider} from 'react-router-dom';
import {NavBar} from './components/navbar.component';
import {TutorialAdd} from './components/tutorial-add.component';
import {TutorialList} from './components/tutorial-list.component';
import {Tutorial} from './components/tutorial.component';
import {DataService} from './shared/services/data.service';
import './index.css';

const pages = ['Tutorials', 'Add Tutorial']
const settings = ['About']

const dataService = new DataService();

const router = createBrowserRouter([
    { path: '/', element: <TutorialList/> },
    { path: '/tutorials', element: <TutorialList/> },
    { path: '/tutorials/add', element: <TutorialAdd/> },
    { path: '/tutorials/:id', element: <Tutorial dataService={dataService} /> },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <Grid container>
            <NavBar pages={pages} settings={settings} />
            <RouterProvider router={router}/>
        </Grid>
    </React.StrictMode>,
);
