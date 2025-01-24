import {LoginRequest} from "../models/LoginRequest";
import AuthService from "../services/AuthService.service";
import {Formik} from "formik";
import * as yup from "yup"
import {Box, Button, TextField, Typography, useTheme} from "@mui/material";
import {tokens} from "../Theme";

export default function Login(){

    const theme = useTheme();
    const colors = tokens(theme.palette.mode);
    
    const authService = new AuthService();

    const initialValues: LoginRequest = {
        username: "",
        password: ""
    }

    const handleFormSubmit = (values: LoginRequest) => {
        authService.login(values);
    };

    const checkoutSchema = yup.object().shape({
        password: yup.string().required("required"),
        username: yup.string().required("required")

    });

    return(
        <Box display={"flex"}
            sx={{
                alignItems: "center",
                justifyContent: "center",
                padding: "3px"
            }}>
            <Formik
                onSubmit={handleFormSubmit}
                initialValues={initialValues}
                validationSchema={checkoutSchema}
            >
                {({
                      values,
                      errors,
                      touched,
                      handleBlur,
                      handleChange,
                      handleSubmit,
                  }) => (
                    <form onSubmit={handleSubmit}>
                        <Box
                            mt={"200px"}
                            display="block"
                            width="300px"
                            gap="30px"
                            p={"10px"}
                            sx={{
                                alignItems: "center",
                                justifyContent: "center"

                            }}

                        >
                            <Typography
                                variant="h2"
                                color={colors.grey[100]}
                                fontWeight="bold"
                                sx={{ m: "0 0 5px 0"}}
                            >
                                Login
                            </Typography>
                            <Box width={"100%"}>
                                <TextField
                                    sx={{
                                        borderRadius: "4px"
                                    }}
                                    fullWidth
                                    variant="filled"
                                    type="text"
                                    label="Username"
                                    onBlur={handleBlur}
                                    onChange={handleChange}
                                    value={values.username}
                                    name="username"
                                    error={!!touched.username && !!errors.username}
                                    helperText={touched.username && errors.username}

                                />
                            </Box>


                            <br/>
                            <TextField
                                fullWidth
                                variant="filled"
                                type="password"
                                label="Password"
                                onBlur={handleBlur}
                                onChange={handleChange}
                                value={values.password}
                                name="password"
                                error={!!touched.password && !!errors.password}
                                helperText={touched.password && errors.password}

                            />

                        
                        </Box>
                        <Box display="flex" justifyContent="center" mt="20px">

                            <Button type="submit" color="secondary" variant="contained">
                                Login
                            </Button>
                        
                        
                        </Box>
                        <Box display="flex" justifyContent="center" mt="20px">

                        <h4>No account? 
                            <a href="/register"> Sign up here</a>
                        </h4>

                        </Box>
                    </form>
                )}
            </Formik>
        </Box>
    );

}