import React from 'react';

import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Grid from "@mui/material/Grid";
import Button from '@mui/material/Button';
import { useState, useContext } from "react";
import Typography from '@mui/material/Typography';
import CloseIcon from '@mui/icons-material/Close';
import Alert from '@mui/material/Alert';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import { Navigate } from 'react-router-dom';

import AuthContext from '../context/AuthContext';
import { translateError } from '../ErrorMessages';

function Login() {
    const [status, setStatus] = useState(null);
    const [alert, setAlert] = useState(null);
    const { login, user } = useContext(AuthContext);

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    const handleUsernameChange = (e) => setUsername(e.target.value);
    const handlePasswordChange = (e) => setPassword(e.target.value);

    async function handleLoginButtonClicked() {
        try {
            let result = await login(username, password);
            if (result.response.ok) {
                setStatus(200);
            } else {
                if (result.response.status === 400) {
                    const key = Object.keys(result.data)[0];
                    setAlert(translateError(result.data[key], result.data[key]));
                } else if (result.response.status === 401) {
                    setAlert("Probléma a bejelentkezéssel, próbáld újra később!");
                } else if (result.response.status === 403) {
                    setAlert("Hibás email cím vagy jelszó, próbáld újra!");
                } else {
                    setAlert("Váratlan hiba történt...");
                }
                setStatus(result.response.status);
            }
        } catch (error) {
            console.error("Error:", error);
            setStatus(500);
            setAlert("Váratlan hiba történt...");
        }
    }

    return (
        <Box sx={{
            width: '100%',
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #003366 30%, #002244 50%, #FFCC00 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            p: 2,
            overflow: 'hidden'
        }}>
            {user ? <Navigate to="/" replace={true} /> : null}
            <Grid container justifyContent="center" sx={{ animation: 'fadeInSlide 1s ease-in-out' }}>
                <Paper elevation={6} sx={{
                    width: '100%',
                    maxWidth: 420,
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    borderRadius: 4,
                    p: 4,
                    border: '2px solid #FFCC00',
                    boxShadow: '0 0 20px rgba(0, 51, 102, 0.3)',
                    animation: 'fadeSlide 0.7s ease-out'
                }}>
                    <Grid container rowSpacing={2} alignItems="center" justifyContent="center" direction="column">
                        <Grid item xs={12}>
                            <Typography variant="h5" align="center" sx={{ color: '#002244', fontWeight: 700, lineHeight: 1.4 }}>
                                🚂 Vasúti utazástervező alkalmazás bejelentkezés
                            </Typography>
                        </Grid>

                        {status && status !== 200 && (
                            <Grid item xs={12} sx={{ width: '100%', animation: 'fadeSlide 0.4s ease-out' }}>
                                <Alert
                                    severity="error"
                                    sx={{ width: '100%', mt: 1, px: 2, boxSizing: 'border-box' }}
                                    action={
                                        <IconButton
                                            aria-label="close"
                                            color="inherit"
                                            size="small"
                                            onClick={() => {
                                                setStatus(null);
                                                setAlert(null);
                                            }}
                                        >
                                            <CloseIcon fontSize="inherit" />
                                        </IconButton>
                                    }
                                >
                                    {alert}
                                </Alert>
                            </Grid>
                        )}

                        <Grid item xs={8} align="center">
                            <TextField
                                fullWidth
                                id="username"
                                label="Email"
                                value={username}
                                onChange={handleUsernameChange}
                            />
                        </Grid>
                        <Grid item xs={8} align="center">
                            <TextField
                                fullWidth
                                id="password"
                                label="Jelszó"
                                type="password"
                                value={password}
                                onChange={handlePasswordChange}
                            />
                        </Grid>
                        <Grid item xs={8}>
                            <Button
                                fullWidth
                                variant="contained"
                                sx={{
                                    mt: 2,
                                    backgroundColor: '#003366',
                                    color: '#FFCC00',
                                    fontWeight: 700,
                                    ':hover': {
                                        backgroundColor: '#002244'
                                    },
                                    py: 1.2
                                }}
                                onClick={handleLoginButtonClicked}
                            >
                                🚇 Bejelentkezés
                            </Button>
                        </Grid>
                    </Grid>
                </Paper>
            </Grid>
            <style>
                {`
                @keyframes fadeSlide {
                    from {
                        opacity: 0;
                        transform: translateY(-10px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }

                @keyframes fadeInSlide {
                    from {
                        opacity: 0;
                        transform: translateY(40px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                `}
            </style>
        </Box>
    );
}

export default Login;
