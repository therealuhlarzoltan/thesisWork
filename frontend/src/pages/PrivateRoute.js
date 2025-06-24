import { Navigate } from "react-router-dom";
import React from 'react';
import { useContext } from "react";
import AuthContext from "../context/AuthContext";

function PrivateRoute(props) {
    let {user} = useContext(AuthContext);
    return (
        user ?
            props.route :
            <Navigate to={"/"} replace={true}/>
    );
}

export default PrivateRoute