import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import Home from './components/Home/Home.jsx';
import Login from './components/Login/Login.jsx'
import Register from './components/Register/Register.jsx';
import Navbar from './components/Navbar/Navbar.jsx'
import Layout from './components/Layout/Layout.jsx'


const router = createBrowserRouter([
  // {
  //   path: "/home",
  //   element: <Home />
  // },
  //   {
  //   path: "/",
  //   element: <Login />
  // },
  // {
  //   path: "login",
  //   element: <Login />
  // },
  //     {
  //   path: "r",
  //   element: <Register />
  // }



  {
        path: '', element: <Layout />, children: [
      { path: '/', element: <Login /> },
      { path: '/login', element: <Login /> },
       {  path: "/home",  element: <Home /> },
      { path: 'r', element: <Register /> },
      // { path: '*', element: <NotFound /> },
   
    ]
  }
])

function App() {

  

  return  <>

<RouterProvider router={router} />
    </>
  
}

export default App
