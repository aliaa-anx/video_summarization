import React, { Component } from 'react'
import Navbar from '../Navbar/Navbar'
import { Outlet } from 'react-router-dom';

export default class Layout extends Component {
    render() {
        return <>
            <Navbar />

            <Outlet/>



        </>
    }
}
