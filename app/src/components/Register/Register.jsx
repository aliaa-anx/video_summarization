import { useFormik } from 'formik'
import React, { useState } from 'react'
import { RotatingLines } from 'react-loader-spinner'
import { useNavigate } from 'react-router-dom'
import { FiMail, FiLock, FiUser, FiKey } from 'react-icons/fi';
import { motion } from 'framer-motion';
import axios from 'axios';
export default function Register() {

  let user = {
    username: "",
    email: "",
    password: ""
  }

  const [errorInLogin, seterrorInLogin] = useState(null)
  const [isLoding, setisLoding] = useState(false)

  const navigateToLogin = useNavigate()



  async function Register(values) {
    // console.log(values);
    
    // setisLoding(true)

     try {
            const data = await axios.post("http://localhost:8080/api/auth/register", values)
            console.log(data,"in register");

                seterrorInLogin("success")         

                setTimeout(function () {
                    navigateToLogin("/")
                }, 1000)
            
        } catch (error) {
            console.log("error in register",error);
            seterrorInLogin(" username or email already exists")
        }
    // setisLoding(false)

  }



  const formikObj = useFormik({
    initialValues: user,
    onSubmit: Register,

    validate: function (values) {

      seterrorInLogin(null)

      const errors = {}

      if (values.username.trim().length < 2 || values.username.trim().length > 15) {
        errors.username = "name must be from 2 characters to 15 characters "
      }

      if (values.email.includes("@") === false || values.email.includes(".") === false) {
        errors.email = "email invalid"
      }
      if (values.password.trim().length < 8 ) {
        errors.password = "password must be at least 8 characters  "
      }

      return errors;
    }

  });

  return <>

    <div className="vh-100  d-flex align-items-center justify-content-center login-register-bg  ">

      <motion.div
        className="login  border-1 p-4 rounded-4"
        initial={{ opacity: 0, y: 100 }}
        animate={{ opacity: 1, y: 0 }}
        whileHover={{ scale: 1.03 }}
        transition={{ duration: 2, type: "spring", scale: { duration: 0.5 } }}
      >

        {errorInLogin != null ? <div className="alert alert-success">{errorInLogin} </div> : ""}
        <h2 className='loginh2'> Register now : </h2>
        <form onSubmit={formikObj.handleSubmit} >
          {/* **************************************** */}
          {/* name */}
          <motion.div
            className="input-group mb-3"
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3, duration: 0.2 }}
          >

            <span className="input-group-text icon-bg">
              <FiUser className="fs-4 icon-color " />
            </span>

            <input
              onBlur={formikObj.handleBlur}
              onChange={formikObj.handleChange}
              value={formikObj.values.username}
              id='username'
              type="text"
              placeholder='name'
              className='form-control'
            />
          </motion.div>
          {formikObj.errors.username && formikObj.touched.username ? <div className="alert alert-danger"> {formikObj.errors?.username}</div> : ""}


          {/*  */}


          {/* email */}
          <motion.div
            className="input-group mb-3"
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.5, duration: 0.2 }}
          >

            <span className="input-group-text icon-bg">
              <FiMail className="fs-4 icon-color " />
            </span>

            <input
              onBlur={formikObj.handleBlur}
              onChange={formikObj.handleChange}
              value={formikObj.values.email}
              id='email'
              type="text"
              placeholder='email'
              className='form-control'
            />
          </motion.div>
          {formikObj.errors.email && formikObj.touched.email ? <div className="alert alert-danger"> {formikObj.errors?.email}</div> : ""}

          {/* password  */}
          <motion.div
            className="input-group mb-3"
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.8, duration: 0.2 }}
          >

            <span className="input-group-text icon-bg">
              <FiLock className="fs-4 icon-color" />
            </span>

            <input onBlur={formikObj.handleBlur}
              onChange={formikObj.handleChange}
              value={formikObj.values.password}
              id='password' type="password" placeholder='password'
              className='form-control'
            />

          </motion.div>
          {formikObj.errors?.password && formikObj.touched.password ? <div className="alert alert-danger"> {formikObj.errors?.password}</div> : ""}
          {/* **************************************** */}


          <motion.div
            className="d-flex justify-content-center py-3"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 1.3, duration: 0.3 }}
          >
            <button type="submit" disabled={formikObj.isValid === false || formikObj.dirty === false}
              className='btn login-btn px-4 py-2 fs-5' > {isLoding ? <RotatingLines
                visible={true}
                height="45"
                width="45"
                color="white"
                strokeWidth="5"
                animationDuration="0.75"
                ariaLabel="rotating-lines-loading"
                wrapperStyle={{}}
                wrapperClass=""
              /> : "Register"}
            </button>
          </motion.div>

        </form>

      </motion.div>


    </div>



  </>
}
