import AdbIcon from '@mui/icons-material/Adb';
import MenuIcon from '@mui/icons-material/Menu';
import {AppBar, Avatar, Box, Container, IconButton, Toolbar, Tooltip, Typography} from '@mui/material';
import Button from '@mui/material/Button';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import {Component, MouseEvent} from 'react';
import {NavigateFunction} from 'react-router-dom';
import {nullable} from '../shared/types/common.type.ts';

type Props = {
    pages: string[],
    settings: string[]
    navigate: NavigateFunction
};
type State = {
    anchorElNav: nullable<HTMLElement>;
    anchorElUser: nullable<HTMLElement>;
};

export class NavBar extends Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = {
            anchorElNav: null,
            anchorElUser: null
        };
    }

    handleOpenNavMenu = (event: MouseEvent<HTMLElement>) => {
        this.setState({
            anchorElNav: event.currentTarget
        });
    }

    handleCloseNavMenu = (page: string) => {
        this.setState({
            anchorElNav: null
        });
        if(page.toLowerCase() === 'tutorials') {
            this.props.navigate('/tutorials')
        } else if(page.toLowerCase() === 'add tutorial') {
            this.props.navigate('/tutorials/add')
        }
    }

    handleOpenUserMenu = (event: MouseEvent<HTMLElement>) => {
        this.setState({
            anchorElUser: event.currentTarget
        });
    }

    handleCloseUserMenu = (setting: string) => {
        this.setState({
            anchorElUser: null
        });
        if(setting.toLowerCase() === 'about') {
            this.props.navigate('/about');
        }
    }

    override render() {
        return (
            <AppBar position='relative'>
                <Container maxWidth='xl'>
                    <Toolbar disableGutters>
                        <AdbIcon sx={{ display: { xs: 'none', md: 'flex' }, mr: 1 }} />
                        <Typography
                            variant="h6"
                            noWrap
                            component="a"
                            href="#app-bar-with-responsive-menu"
                            sx={{
                                mr: 2,
                                display: { xs: 'none', md: 'flex' },
                                fontFamily: 'monospace',
                                fontWeight: 700,
                                letterSpacing: '.3rem',
                                color: 'inherit',
                                textDecoration: 'none',
                            }}
                        >
                            DEMO
                        </Typography>

                        <Box sx={{ flexGrow: 1, display: { xs: 'flex', md: 'none' } }}>
                            <IconButton
                                size="large"
                                aria-label="account of current user"
                                aria-controls="menu-appbar"
                                aria-haspopup="true"
                                onClick={this.handleOpenNavMenu}
                                color="inherit"
                            >
                                <MenuIcon />
                            </IconButton>
                            <Menu
                                id="menu-appbar"
                                anchorEl={this.state.anchorElNav}
                                anchorOrigin={{
                                    vertical: 'bottom',
                                    horizontal: 'left',
                                }}
                                keepMounted
                                transformOrigin={{
                                    vertical: 'top',
                                    horizontal: 'left',
                                }}
                                open={Boolean(this.state.anchorElNav)}
                                onClose={this.handleCloseNavMenu}
                                sx={{
                                    display: { xs: 'block', md: 'none' },
                                }}
                            >
                                {this.props.pages.map((page) => (
                                    <MenuItem key={page} onClick={() => this.handleCloseNavMenu(page)}>
                                        <Typography textAlign="center">{page}</Typography>
                                    </MenuItem>
                                ))}
                            </Menu>
                        </Box>
                        <AdbIcon sx={{ display: { xs: 'flex', md: 'none' }, mr: 1 }} />
                        <Typography
                            variant="h5"
                            noWrap
                            component="a"
                            href="#app-bar-with-responsive-menu"
                            sx={{
                                mr: 2,
                                display: { xs: 'flex', md: 'none' },
                                flexGrow: 1,
                                fontFamily: 'monospace',
                                fontWeight: 700,
                                letterSpacing: '.3rem',
                                color: 'inherit',
                                textDecoration: 'none',
                            }}
                        >
                            LOGO
                        </Typography>
                        <Box sx={{ flexGrow: 1, display: { xs: 'none', md: 'flex' } }}>
                            {this.props.pages.map((page) => (
                                <Button
                                    key={page}
                                    onClick={() => this.handleCloseNavMenu(page)}
                                    sx={{ my: 2, color: 'white', display: 'block' }}
                                >
                                    {page}
                                </Button>
                            ))}
                        </Box>

                        <Box sx={{ flexGrow: 0 }}>
                            <Tooltip title="Open settings">
                                <IconButton onClick={this.handleOpenUserMenu} sx={{ p: 0 }}>
                                    <Avatar alt="Remy Sharp" src="/static/images/avatar/2.jpg" />
                                </IconButton>
                            </Tooltip>
                            <Menu
                                sx={{ mt: '45px' }}
                                id="menu-appbar"
                                anchorEl={this.state.anchorElUser}
                                anchorOrigin={{
                                    vertical: 'top',
                                    horizontal: 'right',
                                }}
                                keepMounted
                                transformOrigin={{
                                    vertical: 'top',
                                    horizontal: 'right',
                                }}
                                open={Boolean(this.state.anchorElUser)}
                                onClose={this.handleCloseUserMenu}
                            >
                                {this.props.settings.map((setting) => (
                                    <MenuItem key={setting} onClick={() => this.handleCloseUserMenu(setting)}>
                                        <Typography textAlign="center">{setting}</Typography>
                                    </MenuItem>
                                ))}
                            </Menu>
                        </Box>
                    </Toolbar>
                </Container>
            </AppBar>
        );
    }
}