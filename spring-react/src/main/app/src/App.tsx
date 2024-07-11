import { CssBaseline, Grid } from '@mui/material';
import { Route, Routes, useNavigate } from 'react-router-dom';
import { About } from './components/about.component.tsx';
import { NavBar } from './components/navbar.component.tsx';
import { TutorialAdd } from './components/tutorial-add.component.tsx';
import { TutorialList } from './components/tutorial-list.component.tsx';
import { Tutorial } from './components/tutorial.component.tsx';
import { DataService } from './shared/services/data.service.ts';

const pages = ['Tutorials', 'Add Tutorial'];
const settings = ['About'];

type Props = {
    dataService: DataService
}

export default function App(props: Readonly<Props>) {
    const navigate = useNavigate();

    return (
        <>
            <CssBaseline/>
            <Grid container spacing={0} direction="column">
                <Grid item>
                    <NavBar pages={pages} settings={settings} navigate={navigate}/>
                </Grid>
                <Grid item>
                    <Routes>
                        <Route path="/" element={<TutorialList dataService={props.dataService}/>}/>
                        <Route path="/about" element={<About/>}/>
                        <Route path="/tutorials" element={<TutorialList dataService={props.dataService}/>}/>
                        <Route path="/tutorials/add" element={<TutorialAdd dataService={props.dataService}/>}/>
                        <Route path="/tutorials/:id" element={<Tutorial dataService={props.dataService}/>}/>
                    </Routes>
                </Grid>
            </Grid>
        </>
    );
}
