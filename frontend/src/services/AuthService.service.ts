import axios from "axios";
import {LoginRequest} from "../models/LoginRequest";
import {RegisterRequest} from "../models/RegisterRequest";
import {toast} from "react-toastify";
import {useNavigate} from "react-router-dom";

const base_url = "http://localhost:8080/api/auth/";

export default class AuthService{
    private navigate = useNavigate();

    signup(registerRequest: RegisterRequest){
        axios.post(base_url + "signup", registerRequest)
            .then(
                (response) => {
                    if(response.data.data){
                        toast.error(response.data.data);
                        return;
                    }
                    else{
                        this.navigate("/verifyaccount");
                    }

                    return response;
                }
            )
    }

    login(loginRequest: LoginRequest){
        axios.post(base_url + "login", loginRequest)
        .then(
            (response) => {
                
                if(response.data.data){
                    toast.error(response.data.data);
                    return;
                }
                else{
                    localStorage.setItem("user", JSON.stringify(response.data));
                    this.navigate("/home");
                }
            }
        )
    }

    getCurrentUsername(){
        const userStr = localStorage.getItem("user");
        if(userStr){
            const user = JSON.parse(userStr);
            return user.username;
        }
    }

    getCurrentJWT(){
        const userStr = localStorage.getItem("user");
        if(userStr){
            const user = JSON.parse(userStr);
            return user.authenticationToken;
        }
    }
}