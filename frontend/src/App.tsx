import {CssBaseline, ThemeProvider} from '@mui/material';
import 'react-toastify/dist/ReactToastify.css';
import {ToastContainer} from 'react-toastify';
import {ColorModeContext, useMode} from './Theme';
import {BrowserRouter, Route, Routes} from 'react-router-dom';
import Entrypoint from './pages/Entrypoint';
import Login from './pages/Login';


function App() {

  const [theme, colorMode] = useMode();
  
  return (
    <>
    {/*An error that's just a pain really, works fine*/}
    {/* @ts-ignore */}
    <ColorModeContext.Provider value={colorMode}>

    {/*An error that's just a pain really, works fine*/}
    {/* @ts-ignore */}
    <ThemeProvider theme={theme}>
      <CssBaseline/>
      
      <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login/>}/>
        <Route path="/" element={<Entrypoint/>}/>        
      </Routes>
      </BrowserRouter>
      
      <ToastContainer
        position="top-right"
        autoClose={1500}
        hideProgressBar
        newestOnTop={true}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        />
    </ThemeProvider>
    </ColorModeContext.Provider>
    
    </>
  )
}

export default App