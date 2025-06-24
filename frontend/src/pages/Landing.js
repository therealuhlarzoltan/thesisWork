import React from 'react';

import Paper from '@mui/material/Paper';
import { Grid, Typography } from "@mui/material";
import Button from '@mui/material/Button';
import Stack from "@mui/material/Stack";
import LoginIcon from '@mui/icons-material/Login';
import PersonAddAltIcon from '@mui/icons-material/PersonAddAlt';

import { useNavigate } from "react-router-dom";
import { purple, blue } from "@mui/material/colors";

function Landing() {

    const navigate = useNavigate();
    const buttons = [
        <Button
            key="Bejelentkezés"
            variant="contained"
            startIcon={<LoginIcon />}
            sx={{
                color: "white",
                backgroundColor: purple['500'],
                ':hover': {
                    backgroundColor: purple['700'],
                    transform: 'scale(1.05)',
                    transition: 'all 0.3s ease-in-out',
                    boxShadow: '0 0 12px rgba(128, 0, 128, 0.6)'
                },
                px: 4,
                py: 1.5,
                fontWeight: 700,
                borderRadius: 3
            }}
            onClick={() => navigate("/login")}
        >
            Bejelentkezés
        </Button>,
        <Button
            key="Regisztráció"
            variant="contained"
            startIcon={<PersonAddAltIcon />}
            sx={{
                color: "white",
                backgroundColor: blue['700'],
                ':hover': {
                    backgroundColor: blue['900'],
                    transform: 'scale(1.05)',
                    transition: 'all 0.3s ease-in-out',
                    boxShadow: '0 0 12px rgba(0, 51, 102, 0.6)'
                },
                px: 4,
                py: 1.5,
                fontWeight: 700,
                borderRadius: 3
            }}
            onClick={() => navigate("/register")}
        >
            Regisztráció
        </Button>
    ];

    return (
        <Grid
            container
            justifyContent="center"
            alignItems="center"
            sx={{
                height: "100vh",
                background: 'linear-gradient(to bottom right, #003366 0%, #002244 50%, #FFCC00 100%)',
                padding: 2
            }}
        >
            <Grid item xs={10} sm={8} md={6} lg={4} sx={{ animation: 'fadeInSlide 1s ease-in-out' }}>
                <Paper
                    elevation={12}
                    sx={{
                        padding: 5,
                        borderRadius: 4,
                        backgroundColor: 'rgba(255, 255, 255, 0.97)',
                        textAlign: 'center',
                        border: '3px solid #FFCC00',
                        boxShadow: '0 8px 30px rgba(0, 0, 0, 0.2)',
                        animation: 'fadeSlide 0.7s ease-out'
                    }}
                >
                    <Typography
                        variant="h4"
                        gutterBottom
                        sx={{
                            fontWeight: 'bold',
                            color: '#002244',
                            textShadow: '1px 1px 2px rgba(0, 0, 0, 0.2)'
                        }}
                    >
                        🚂 Vasúti utazástervező alkalmazás késés előrejelzéssel
                    </Typography>
                    <Stack spacing={3} direction="row" justifyContent="center" sx={{ mt: 4 }}>
                        {buttons}
                    </Stack>
                </Paper>
            </Grid>
            <style>
                {`
                @keyframes fadeSlide {
                    from {
                        opacity: 0;
                        transform: translateY(20px);
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
        </Grid>
    );
}

export default Landing;