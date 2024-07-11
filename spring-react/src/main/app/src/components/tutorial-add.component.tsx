import { Box, Snackbar, TextField } from '@mui/material';
import Button from '@mui/material/Button';
import { Component } from 'react';
import { DataService } from '../shared/services/data.service.ts';

type Props = {
    dataService: DataService
};

type State = {
    title: string
    description: string,
    isOpen: boolean,
    message: string
};

export class TutorialAdd extends Component<Props, State> {

    private EMPTY_VALUE = '---';

    constructor(props: Props) {
        super(props);
        this.state = {
            title: this.EMPTY_VALUE,
            description: this.EMPTY_VALUE,
            isOpen: false,
            message: this.EMPTY_VALUE
        };
    }

    onSubmit = (target: string) => {
        console.log(`... on submit ${target} ...`);
    };

    onTitleChange = (title?: string) => {
        if (!title) return;
        this.setState({title});
    };

    onDescriptionChange = (desc?: string) => {
        if (!desc) return;
        this.setState({description: desc});
    };

    private checkValue(value?: string): boolean {
        return !!value && value.length > 0 && value !== this.EMPTY_VALUE;
    }

    onPublish = () => {
        if (this.checkValue(this.state.title) && this.checkValue(this.state.description)) {
            this.props.dataService.postTutorial(this.state.title, this.state.description).then(() => {
                const msg = '... published ...';
                console.log(msg);
                this.setState({isOpen: true, message: msg});
            });
        } else {
            console.warn('... no title or description ...');
        }
    };

    onClose = () => {
        this.setState(prevState => {
            return {
                isOpen: !prevState.isOpen
            };
        });
    };

    override render() {
        const padding = '8px';
        return (
            <Box sx={{
                minWidth: 120,
                margin: padding
            }}>
                <form autoComplete="off" onSubmit={e => this.onSubmit(e.currentTarget.target)}>
                    <h4>Add Tutorial</h4>
                    <TextField
                        fullWidth
                        type="text"
                        label="Title"
                        margin="dense"
                        component="form"
                        onChange={e => this.onTitleChange(e.target.value)}
                    />

                    <TextField
                        fullWidth
                        type="text"
                        label="Description"
                        margin="dense"
                        component="form"
                        onChange={e => this.onDescriptionChange(e.target.value)}
                    />
                    <Button variant="contained" sx={{margin: padding}} onClick={this.onPublish}>PUBLISH</Button>
                    <Snackbar
                        open={this.state.isOpen}
                        autoHideDuration={5000}
                        onClose={this.onClose}
                        message={this.state.message}
                    />
                </form>
            </Box>
        );
    }
}
