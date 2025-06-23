import React, { Component } from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider, } from "react-router-dom";

import { AuthProvider } from "./context/AuthContext";

import Login from "./pages/Login";
import Home from "./pages/Home";
import Logout from "./pages/Logout";
import Register from "./pages/Register";
import PrivateRoute from "./pages/PrivateRoute";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Home />
  },
  {
    path: "login/",
    element: <Login />
  },
  {
    path: "logout/",
    element: <PrivateRoute route={<Logout />} />
  },
  {
    path: "register/",
    element: <Register />
  },
]);

export default function App() {
  return (
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
  );
}

