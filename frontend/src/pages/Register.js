import React, { useState, useContext } from 'react';
import {
    Box,
    Grid,
    Paper,
    TextField,
    Typography,
    Button,
    Alert,
    IconButton
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { Navigate } from 'react-router-dom';
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
                setStatus(200);
            } else {
                if (result.response.status === 400) {
                    const key = Object.keys(result.data)[0];
                    setAlert(translateError(result.data[key], result.data[key]));
                    setStatus(result.response.status);
                } else if (result.response.status === 403) {
                    setAlert("Ez az email cím már foglalt.");
                    setStatus(result.response.status);
                } else {
                    setAlert(" Váratlan hiba történt...");
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
            p: 2
        }}>
            {user ? <Navigate to="/" replace={true} /> : null}
            <Grid container justifyContent="center">
                <Paper elevation={6} sx={{
                    width: '100%',
                    maxWidth: 420,
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    borderRadius: 4,
                    p: 4,
                    border: '2px solid #FFCC00'
                }}>
                    <Typography variant="h4" sx={{ color: '#002244', fontWeight: 700, mb: 2 }}>
                        👤 Regisztráció
                    </Typography>
                    {status && status !== 200 && (
                        <Alert
                            severity="error"
                            sx={{ mb: 2 }}
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
                    )}
                    <TextField
                        fullWidth
                        label="Email"
                        variant="outlined"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        sx={{ mb: 2 }}
                    />
                    <TextField
                        fullWidth
                        label="Jelszó"
                        type="password"
                        variant="outlined"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        sx={{ mb: 2 }}
                    />
                    <TextField
                        fullWidth
                        label="Jelszó megerősítése"
                        type="password"
                        variant="outlined"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        sx={{ mb: 3 }}
                    />
                    <Button
                        fullWidth
                        variant="contained"
                        sx={{
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
                </Paper>
            </Grid>
        </Box>
    );
}

export default Register;