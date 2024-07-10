import {Grid} from '@mui/material';
import {Component} from 'react';
import {RouteMatch} from 'react-router';
import {DataService} from '../shared/services/data.service';
import {TutorialType} from '../shared/types/tutorial.type';

type Props = {
    dataService: DataService
    match?: RouteMatch
};

type State = {
    tutorial: TutorialType;
    message?: string;
};

export class Tutorial extends Component<Props, State> {
    
    private readonly id: string

    constructor(props: Props) {
        super(props);
        this.id = this.props.match!.params.id!
        this.state = {
            tutorial: {},
            message: undefined
        }
    }

    private getTutorial(id: string): void {
        this.props.dataService.getTutorial(id).then(tutorial => this.setState({ tutorial }))
    }

    componentDidMount() {
        this.getTutorial(this.id)
    }

    render() {
        // const current = this.state.tutorial
        return (
            <Grid container direction='row' justifyContent='center' alignItems='center'>

            </Grid>
        );
    }
}