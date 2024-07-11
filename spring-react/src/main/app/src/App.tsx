import {Container} from '@mui/material';
import {createBrowserRouter, RouterProvider} from 'react-router-dom';
import {NavBar} from './components/navbar.component.tsx';
import {TutorialAdd} from './components/tutorial-add.component.tsx';
import {TutorialList} from './components/tutorial-list.component.tsx';
import {Tutorial} from './components/tutorial.component.tsx';
import {DataService} from './shared/services/data.service.ts';

const pages = ['Tutorials', 'Add Tutorial']
const settings = ['About']

const dataService = new DataService()

const router = createBrowserRouter([
    { path: '/', element: <TutorialList/> },
    { path: '/tutorials', element: <TutorialList/> },
    { path: '/tutorials/add', element: <TutorialAdd/> },
    { path: '/tutorials/:id', element: <Tutorial dataService={dataService} /> },
]);

export default function App() {
    return (
        <Container maxWidth={false}>
            <NavBar pages={pages} settings={settings} />
            <RouterProvider router={router} />
        </Container>
    );
}