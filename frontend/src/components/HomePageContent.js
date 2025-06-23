import React from 'react';
import { useState, useEffect, useContext } from "react";
import AuthContext from "../context/AuthContext";


function HomePageContent(props) {


    let { user, authTokens } = useContext(AuthContext);


    return (

       <h1>hi there, {user}!</h1>

    );
}

export default HomePageContent