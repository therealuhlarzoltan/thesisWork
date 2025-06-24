import React, { useState, useEffect, useContext } from 'react';
import {
    Box,
    Grid,
    Typography,
    Paper,
    Button,
    TextField,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Alert,
    CircularProgress,
    Collapse,
    Fade,
    Grow
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import AuthContext from "../context/AuthContext";
import TrainIcon from '@mui/icons-material/Train';
import DirectionsRailwayIcon from '@mui/icons-material/DirectionsRailway';
import SearchIcon from '@mui/icons-material/Search';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import dayjs from 'dayjs';
import { translateError } from "../ErrorMessages";

function HomePageContent() {
    const { user, logout, authTokens } = useContext(AuthContext);
    const [from, setFrom] = useState('');
    const [to, setTo] = useState('');
    const [departureTime, setDepartureTime] = useState(null);
    const [arrivalTime, setArrivalTime] = useState(null);
    const [maxChanges, setMaxChanges] = useState(0);
    const [results, setResults] = useState([]);
    const [searching, setSearching] = useState(false);
    const [searchInitiated, setSearchInitiated] = useState(false);
    const [error, setError] = useState(null);

    const handleDepartureChange = (newValue) => {
        setDepartureTime(newValue);
        if (newValue) setArrivalTime(null);
    };

    const handleArrivalChange = (newValue) => {
        setArrivalTime(newValue);
        if (newValue) setDepartureTime(null);
    };

    const isRouteValid = (trains) => {
        for (let i = 1; i < trains.length; i++) {
            const prevTime = getMostAccurateTime(trains[i - 1], 'to');
            const nextTime = getMostAccurateTime(trains[i], 'from');
            if (!prevTime || !nextTime) continue;
            const diff = Math.abs(dayjs(nextTime).diff(dayjs(prevTime), 'minute'));
            if (diff >= 400) return false;
        }
        return true;
    };

    const handleSearch = async () => {
        if (!from || !to) return;
        setSearching(true);
        setResults([]);
        setSearchInitiated(true);
        setError(null);

        const params = new URLSearchParams({
            from,
            to,
            ...(departureTime && !arrivalTime && { departureTime: dayjs(departureTime).format('YYYY-MM-DDTHH:mm') }),
            ...(arrivalTime && !departureTime && { arrivalTime: dayjs(arrivalTime).format('YYYY-MM-DDTHH:mm') }),
            ...(maxChanges > 0 && { maxChanges: maxChanges.toString() })
        });

        try {
            const response = await fetch(
                `https://localhost:8443/route-planner/route/plan?${params.toString()}`,
                {
                    method: 'GET',
                    headers: {
                        Authorization: `Bearer ${authTokens.accessToken}`,
                        'Content-Type': 'application/json'
                    }
                }
            );

            if (response.ok) {
                const data = await response.json();
                const sorted = data
                    .map(route => {
                        const sortedTrains = [...route.trains].sort((a, b) => new Date(a.fromTimeScheduled) - new Date(b.fromTimeScheduled));
                        return { ...route, trains: sortedTrains };
                    })
                    .filter(route => isRouteValid(route.trains))
                    .sort((a, b) => new Date(a.trains[0].fromTimeScheduled) - new Date(b.trains[0].fromTimeScheduled));
                setResults(sorted);
            } else {
                const errorData = await response.json();
                const key = Object.keys(errorData)[0];
                const message = errorData[key];

                if (response.status === 400 || response.status === 422) {
                    setError(translateError(message, message));
                } else {
                    setError("Váratlan hiba történt...");
                }
            }
        } catch (err) {
            setError(`The error that occurred: ${err.message}`);
        } finally {
            setSearching(false);
        }
    };

    const formatTime = (isoString) => {
        if (!isoString || isNaN(Date.parse(isoString))) return '';
        return dayjs(isoString).format('HH:mm');
    };

    const getColor = (actualOrPredicted, scheduled) => {
        if (!actualOrPredicted || !scheduled || isNaN(Date.parse(actualOrPredicted)) || isNaN(Date.parse(scheduled))) return 'inherit';
        return dayjs(actualOrPredicted).isSame(dayjs(scheduled)) ? 'green' : 'red';
    };

    const getMostAccurateTime = (train, type) => {
        return train[`${type}TimeActual`] || train[`${type}TimePredicted`] || train[`${type}TimeScheduled`];
    };

    const getTransferWarning = (prevTrain, nextTrain) => {
        const prevArrival = getMostAccurateTime(prevTrain, 'to');
        const nextDeparture = getMostAccurateTime(nextTrain, 'from');

        if (!prevArrival || !nextDeparture) return null;

        const diffMinutes = dayjs(nextDeparture).diff(dayjs(prevArrival), 'minute');

        if (diffMinutes < 2) {
            return <Alert severity="error" sx={{ my: 1 }}>Meghiúsuló átszállás ({diffMinutes} perc)</Alert>;
        } else if (diffMinutes <= 6) {
            return <Alert severity="warning" sx={{ my: 1 }}>Rizikós átszállási idő: {diffMinutes} perc</Alert>;
        } else {
            return null;
        }
    };

    return (
        <Fade in={true} timeout={600}>
            <Box sx={{ width: '100%', minHeight: '100vh', background: 'linear-gradient(135deg, #003366 30%, #002244 50%, #FFCC00 100%)', p: 3 }}>
                <Paper elevation={6} sx={{ maxWidth: 900, mx: 'auto', p: 4, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.95)', mb: 4 }}>
                    <Grid container justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
                        <Typography variant="h5" fontWeight={700} color="#002244">
                            Bejelentkezve mint <span style={{ color: '#003366' }}>{user}</span>
                        </Typography>
                        <Button variant="outlined" color="error" startIcon={<LogoutIcon />} onClick={logout}>
                            Kijelentkezés
                        </Button>
                    </Grid>

                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth label="Indulási állomás" value={from} onChange={(e) => setFrom(e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth label="Érkezési állomás" value={to} onChange={(e) => setTo(e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <DateTimePicker
                                label="Indulás ideje"
                                value={departureTime}
                                onChange={handleDepartureChange}
                                slotProps={{ textField: { fullWidth: true } }}
                                disabled={Boolean(arrivalTime)}
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <DateTimePicker
                                label="Érkezés ideje"
                                value={arrivalTime}
                                onChange={handleArrivalChange}
                                slotProps={{ textField: { fullWidth: true } }}
                                disabled={Boolean(departureTime)}
                            />
                        </Grid>
                        <Grid item xs={12}>
                            <Grid container spacing={2}>
                                <Grid item xs={6}>
                                    <TextField
                                        fullWidth
                                        label="Max. átszállások"
                                        type="number"
                                        inputProps={{ min: 0 }}
                                        value={maxChanges}
                                        onChange={(e) => setMaxChanges(parseInt(e.target.value) || 0)}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <Button
                                        fullWidth
                                        variant="contained"
                                        startIcon={<SearchIcon />}
                                        sx={{ backgroundColor: '#003366', color: '#FFCC00', fontWeight: 700, py: 1.2, ':hover': { backgroundColor: '#002244' } }}
                                        onClick={handleSearch}
                                        disabled={searching}
                                    >
                                        Keresés
                                    </Button>
                                </Grid>
                            </Grid>
                        </Grid>
                    </Grid>

                    {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
                </Paper>

                <Collapse in={searchInitiated}>
                    <Grow in={true} timeout={700}>
                        <Paper elevation={6} sx={{ maxWidth: 900, mx: 'auto', p: 4, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.95)' }}>
                            {searching ? (
                                <Box display="flex" justifyContent="center" alignItems="center" py={5}>
                                    <CircularProgress color="primary" />
                                </Box>
                            ) : (
                                <>
                                    <Typography variant="h6" fontWeight={600} gutterBottom>
                                        Útvonal találatok:
                                    </Typography>

                                    {results.length === 0 ? (
                                        <Typography>Nincs találat.</Typography>
                                    ) : (
                                        results.map((route, index) => (
                                            <Accordion
                                                key={index}
                                                sx={{
                                                    mb: 2,
                                                    border: '1px solid #ccc',
                                                    borderRadius: 2,
                                                    transition: 'all 0.3s ease-in-out',
                                                    '&:hover': {
                                                        transform: 'scale(1.01)',
                                                        boxShadow: 3
                                                    }
                                                }}
                                            >
                                                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                                    <Typography>
                                                        <TrainIcon sx={{ mr: 1 }} />
                                                        {route.trains[0]?.fromStation} → {route.trains[route.trains.length - 1]?.toStation} ({route.trains.length - 1} átszállás)
                                                    </Typography>
                                                </AccordionSummary>
                                                <AccordionDetails>
                                                    {route.trains.map((train, tIdx) => (
                                                        <React.Fragment key={tIdx}>
                                                            {tIdx > 0 && <Box sx={{ width: '100%' }}>{getTransferWarning(route.trains[tIdx - 1], train)}</Box>}
                                                            <Paper sx={{ p: 2, mb: 1, backgroundColor: '#f5f5f5' }}>
                                                                <Typography variant="body1" fontWeight={600}>
                                                                    <DirectionsRailwayIcon sx={{ mr: 1 }} />
                                                                    {train.trainNumber} ({train.lineNumber})
                                                                </Typography>
                                                                <Typography variant="body2">
                                                                    {train.fromStation} ({formatTime(train.fromTimeScheduled)}) → {train.toStation} ({formatTime(train.toTimeScheduled)})
                                                                </Typography>
                                                                {train.fromTimeActual && train.toTimeActual ? (
                                                                    <Typography variant="body2" color="text.secondary">
                                                                        Tényleges indulás: <span style={{ color: getColor(train.fromTimeActual, train.fromTimeScheduled) }}>{formatTime(train.fromTimeActual)}</span> |
                                                                        Érkezés: <span style={{ color: getColor(train.toTimeActual, train.toTimeScheduled) }}>{formatTime(train.toTimeActual)}</span>
                                                                    </Typography>
                                                                ) : (train.fromTimePredicted && train.toTimePredicted ? (
                                                                    <Typography variant="body2" color="text.secondary">
                                                                        Előrejelzett indulás: <span style={{ color: getColor(train.fromTimePredicted, train.fromTimeScheduled) }}>{formatTime(train.fromTimePredicted)}</span> |
                                                                        Érkezés: <span style={{ color: getColor(train.toTimePredicted, train.toTimeScheduled) }}>{formatTime(train.toTimePredicted)}</span>
                                                                    </Typography>
                                                                ) : null)}
                                                            </Paper>
                                                        </React.Fragment>
                                                    ))}
                                                </AccordionDetails>
                                            </Accordion>
                                        ))
                                    )}
                                </>
                            )}
                        </Paper>
                    </Grow>
                </Collapse>
            </Box>
        </Fade>
    );
}

export default HomePageContent;
