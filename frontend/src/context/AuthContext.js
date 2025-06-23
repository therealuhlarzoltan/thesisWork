import { createContext } from "react";
import { useState, useEffect } from "react";
import React from 'react';
import { jwtDecode } from 'jwt-decode';

const AuthContext = createContext()

export default AuthContext

export const AuthProvider = ({ children }) => {

    let [authTokens, setAuthTokens] = useState(() => localStorage.getItem("authTokens") ? JSON.parse(localStorage.getItem("authTokens")) : null)
    let [user, setUser] = useState(() => localStorage.getItem("authTokens") ? jwtDecode(localStorage.getItem("authTokens")) : null)
    const [loaded, setLoaded] = useState(false)

    let login = async (username, password) => {
        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ "username": username, "password": password })
        }
        let response = await fetch('/security/login/', requestOptions)
        let data = await response.json()

        if (response.status === 200) {
            setAuthTokens(data);
            setUser(jwtDecode(data.access))
            localStorage.setItem("authTokens", JSON.stringify(data));
        }

        return {
            "data": data,
            "response": response
        }

    }

    let logout = async () => {
        try {
            const requestOptions = {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    'Authorization': 'Bearer ' + String(authTokens?.accessToken),
                },
                body: JSON.stringify({
                    "refresh_token": authTokens?.refresh
                })
            }
            let response = await fetch('/security/logout/', requestOptions)
        }  catch (error) {
            return {"error": true}
        }
        setUser(null);
        setAuthTokens(null);
        localStorage.removeItem("authTokens");
        window.location.href = "/";
        return {"error": false}
    }

    let update = async () => {
        try {
            const requestOptions = {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ "refresh": authTokens?.Token })
            }
            let response = await fetch('/security/refresh/', requestOptions)
            let data = await response.json()

            if (response.ok) {
                setAuthTokens(data);
                setUser(jwtDecode(data.access))
                localStorage.setItem("authTokens", JSON.stringify(data));
                setLoaded(true);
            }
            else {
                setUser(null);
                setAuthTokens(null);
                localStorage.removeItem("authTokens");
                window.location.href = "/";
            }
        } catch (error) {
            setUser(null);
            setAuthTokens(null);
            localStorage.removeItem("authTokens");
            window.location.href = "/";
        }

        setLoaded(true);
    }

    let contextData = {
        "login": login,
        "user": user,
        "logout": logout,
        "update": update,
        "authTokens": authTokens,
    }

    useEffect(()=> {

        if (!loaded && authTokens) {
            update();
        }
        else {
            setLoaded(true);
        }

        let interval = setInterval(() => {
            if (authTokens)
            {
                update();
            }
        }, 240000);
        return () => clearInterval(interval);

    }, [authTokens, loaded])


    return (
        <AuthContext.Provider value={contextData}>
            { loaded ? children : null}
        </AuthContext.Provider>
    );
}