import { Box, FormControl, InputLabel, Select, TextField } from '@mui/material';
import MenuItem from '@mui/material/MenuItem';
import { Component } from 'react';
import { DataService } from '../shared/services/data.service';

type Props = {
    dataService: DataService
};

type State = {
    tutorials: string[],
    selectedTitle: string,
    selectedDescription: string
};

export class TutorialList extends Component<Props, State> {
    private readonly DEFAULT = ['---'];
    private readonly EMPTY_VALUE = this.DEFAULT[0];

    constructor(props: Props) {
        super(props);
        this.state = {
            tutorials: this.DEFAULT,
            selectedTitle: 'None',
            selectedDescription: this.EMPTY_VALUE
        };
    }

    onSelectOpen = () => {
        console.log('... on select ...');
        this.props.dataService.getTutorials().then(tutorials => {
            this.setState({
                tutorials: this.DEFAULT.concat(tutorials.filter(t => !!t.title).map(t => t.title!))
            });
        });
    };

    onSelectChange = (title: string) => {
        this.props.dataService.getTutorialByTitle(title).then(tutorial => {
            this.setState({
                selectedTitle: tutorial.title ?? this.EMPTY_VALUE,
                selectedDescription: tutorial.description ?? this.EMPTY_VALUE
            });
        });
    };

    onSubmit = () => {
        console.log('... on submit ...');
    };

    showTutorials = () => {
        return this.state.tutorials.map((title, idx) => {
            return (
                <MenuItem key={`${title}.${idx}`} value={title}>{title.toUpperCase()}</MenuItem>
            );
        });
    };

    onTitleChange = (title: string) => {
        console.log(`... title changed to ${title} ...`);
        this.setState({
            selectedTitle: title
        });
    };

    onDescChange = (desc: string) => {
        console.log(`... description changed to ${desc} ...`);
        this.setState({
            selectedDescription: desc
        });
    };

    override render() {
        const padding = '8px'
        return (
            <Box sx={{
                minWidth: 120,
                margin: padding,
                padding: padding
            }}>
                <FormControl fullWidth>
                    <InputLabel id="select-tutorials-label">Tutorials</InputLabel>
                    <Select onOpen={this.onSelectOpen} onChange={e => this.onSelectChange(e.target.value as string)}>
                        {this.showTutorials()}
                    </Select>
                    <form autoComplete="off" onSubmit={this.onSubmit}>
                        <h4>{this.state.selectedTitle === this.EMPTY_VALUE ? 'NONE' : this.state.selectedTitle.toUpperCase()}</h4>
                        <TextField
                            fullWidth
                            type="text"
                            label="Title"
                            margin="dense"
                            component="form"
                            value={this.state.selectedTitle || this.EMPTY_VALUE}
                            onChange={e => this.onTitleChange(e.target.value)}
                        />

                        <TextField
                            fullWidth
                            type="text"
                            label="Description"
                            margin="dense"
                            component="form"
                            value={this.state.selectedDescription || this.EMPTY_VALUE}
                            onChange={e => this.onDescChange(e.target.value)}
                        />
                    </form>
                </FormControl>
            </Box>
        );
    }

}
