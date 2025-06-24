import React, { useState, useContext } from 'react';
import {
    Box,
    Grid,
    Paper,
    TextField,
    Typography,
    Button,
    Alert,
    IconButton,
    Link
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import DirectionsRailwayFilledIcon from '@mui/icons-material/DirectionsRailwayFilled';
import { Navigate, Link as RouterLink } from 'react-router-dom';
import AuthContext from '../context/AuthContext';
import { translateError } from '../ErrorMessages';

function Register() {
    const { register, user } = useContext(AuthContext);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [status, setStatus] = useState(null);
    const [alert, setAlert] = useState(null);

    const handleSubmit = async () => {
        if (password !== confirmPassword) {
            setAlert("A jelszavak nem egyeznek meg!");
            setStatus(400);
            return;
        }

        try {
            const result = await register(email, password);
            if (result.response.ok) {
                setEmail("");
                setPassword("");
                setConfirmPassword("");
                setStatus(200);
                setAlert(null);
            } else {
                if (result.response.status === 400) {
                    const key = Object.keys(result.data)[0];
                    setAlert(translateError(result.data[key], result.data[key]));
                    setStatus(result.response.status);
                } else if (result.response.status === 403) {
                    setAlert("Ez az email cím már foglalt.");
                    setStatus(result.response.status);
                } else {
                    setAlert("Váratlan hiba történt...");
                    setStatus(result.response.status);
                }
            }
        } catch (error) {
            console.error("The exception that occurred:", error);
            setStatus(500);
            setAlert("Váratlan hiba történt...");
        }
    };

    return (
        <Box sx={{
            width: '100%',
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #003366 0%, #002244 50%, #FFCC00 100%)',
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
                    maxWidth: 640,
                    minWidth: 420,
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    borderRadius: 4,
                    p: 4,
                    border: '2px solid #FFCC00',
                    boxShadow: '0 0 20px rgba(0, 51, 102, 0.3)',
                    animation: 'fadeSlide 0.7s ease-out'
                }}>
                    <Grid container rowSpacing={2} direction="column" alignItems="center" justifyContent="center">
                        <Grid item xs={12} sx={{ width: '100%' }}>
                            <Typography variant="h4" sx={{ color: '#002244', fontWeight: 700, mb: 1 }}>
                                👤 Regisztráció
                            </Typography>
                        </Grid>

                        {status && (
                            <Grid item xs={12} sx={{ width: '100%', animation: 'fadeSlide 0.4s ease-out' }}>
                                <Alert
                                    severity={status === 200 ? 'success' : 'error'}
                                    icon={status === 200 ? <DirectionsRailwayFilledIcon fontSize="inherit" sx={{ animation: 'bounce 1.2s infinite' }} /> : undefined}
                                    sx={{ width: '100%', px: 2, boxSizing: 'border-box' }}
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
                                    {status === 200 ? (
                                        <span>
                                            <strong>Sikeres regisztráció!</strong> Most már{' '}
                                            <Link component={RouterLink} to="/login" underline="hover" fontWeight="bold">
                                                bejelentkezhetsz
                                            </Link>
                                            .
                                        </span>
                                    ) : alert}
                                </Alert>
                            </Grid>
                        )}

                        <Grid item xs={12} sx={{ width: '100%' }}>
                            <TextField
                                fullWidth
                                label="Email"
                                variant="outlined"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </Grid>
                        <Grid item xs={12} sx={{ width: '100%' }}>
                            <TextField
                                fullWidth
                                label="Jelszó"
                                type="password"
                                variant="outlined"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </Grid>
                        <Grid item xs={12} sx={{ width: '100%' }}>
                            <TextField
                                fullWidth
                                label="Jelszó megerősítése"
                                type="password"
                                variant="outlined"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                            />
                        </Grid>
                        <Grid item xs={12} sx={{ width: '100%' }}>
                            <Button
                                fullWidth
                                variant="contained"
                                sx={{
                                    mt: 2,
                                    backgroundColor: '#003366',
                                    color: '#FFCC00',
                                    fontWeight: 700,
                                    ':hover': { backgroundColor: '#002244' },
                                    py: 1.2
                                }}
                                onClick={handleSubmit}
                            >
                                Regisztráció
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

                @keyframes bounce {
                    0%, 100% {
                        transform: translateY(0);
                    }
                    50% {
                        transform: translateY(-4px);
                    }
                }
                `}
            </style>
        </Box>
    );
}

export default Register;
