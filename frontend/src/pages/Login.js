import React from 'react';

import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Grid from "@mui/material/Grid";
import Button from "@mui/material/Button";
import { useState } from "react";
import Typography from '@mui/material/Typography';
import CloseIcon from '@mui/icons-material/Close';
import Alert from '@mui/material/Alert';
import IconButton from '@mui/material/IconButton';

import { useContext } from 'react';
import AuthContext from '../context/AuthContext';

import { Navigate } from 'react-router-dom';

import { Paper } from '@mui/material';
import { translateError } from '../ErrorMessages';

function Login() {

    const [status, setStatus] = useState(null)
    const [alert, setAlert] = useState(null)

    let { login } = useContext(AuthContext);
    let { user } = useContext(AuthContext);

    const [username, setUsername] = useState("");
    const handleUsernameChange = (event) => {
        setUsername(event.target.value)
    };

    const [password, setPassword] = useState("");
    const handlePasswordChange = (event) => {
        setPassword(event.target.value);
    };

    async function handleLoginButtonClicked() {
        try {
            let result = await login(username, password)
            if (result.response.ok) {
                setStatus(200);
            }
            else {
                if (result.response.status === 400) {
                    const key = Object.keys(result.data)[0];
                    setAlert(translateError(result.data[key], result.data[key]));
                    setStatus(result.response.status);
                } else if (result.response.status === 401) {
                    setAlert("\uD83D\uDEA7 Probléma a bejelentkezéssel, próbáld újra később!")
                    setStatus(result.response.status)
                } else if (result.response.status === 403) {
                    setAlert("\u274C Hibás email cím vagy jelszó, próbáld újra!")
                    setStatus(result.response.status)
                } else {
                    setAlert("\u26A0\uFE0F Váratlan hiba történt...")
                    setStatus(result.response.status)
                }
            }
        } catch (error) {
            console.log("the error that occurred is: ", error)
            setStatus(500)
            setAlert("\u26A0\uFE0F Váratlan hiba történt...")
        }
    }

    return (
        <Box sx={{
            width: '100%',
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #003366 30%, #002244 70%, #FFCC00 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            p: 2
        }}>
            {user ? <Navigate to={"/"} replace={true} /> : null}
            <Grid container rowSpacing={1.5} alignItems="center" justifyContent="center" direction="column">
                <Grid item xs={12} id="alert-grid">
                    {status === 200 ? <Navigate to={"/"} replace={true} /> : null}
                    {status && status !== 200 ?
                        <Alert severity="error" sx={{ margin: "auto", mb: 2, maxWidth: '400px' }}
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
                        </Alert> : null
                    }
                </Grid>
                <Paper elevation={6} sx={{
                    width: "100%",
                    maxWidth: 420,
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    borderRadius: 4,
                    p: 4,
                    border: '2px solid #FFCC00',
                    boxShadow: '0 0 20px rgba(0, 51, 102, 0.3)'
                }}>
                    <Grid container rowSpacing={2} alignItems="center" justifyContent="center" direction="column">
                        <Grid item xs={12}>
                            <Typography variant="h5" gutterBottom align="center" sx={{ color: '#002244', fontWeight: 700, lineHeight: 1.4 }}>
                                🚂 Vasúti utazástervező alkalmazás bejelentkezés
                            </Typography>
                        </Grid>
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
                                sx={{
                                    marginTop: 2,
                                    backgroundColor: '#003366',
                                    color: '#FFCC00',
                                    fontWeight: 700,
                                    ':hover': {
                                        backgroundColor: '#002244'
                                    },
                                    py: 1.2
                                }}
                                size="large"
                                variant="contained"
                                onClick={handleLoginButtonClicked}
                            >
                                🚇 Bejelentkezés
                            </Button>
                        </Grid>
                    </Grid>
                </Paper>
            </Grid>
        </Box>
    );
}

export default Login;
