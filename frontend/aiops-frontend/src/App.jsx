import { BrowserRouter, Routes, Route } from "react-router-dom";

import Sidebar from "./components/Sidebar";
import Dashboard from "./pages/Dashboard";
import Devices from "./pages/Devices";
import Metrics from "./pages/Metrics";
import Alerts from "./pages/Alerts";
import Predictions from "./pages/Predictions";
import RootCause from "./pages/RootCause";
import SelfHealing from "./pages/SelfHealing";

import "./App.css";

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <Sidebar />

        <main className="main-content">
          <Routes>
            <Route
              path="/"
              element={<Dashboard />}
            />

            <Route
              path="/devices"
              element={<Devices />}
            />

            <Route
              path="/metrics"
              element={<Metrics />}
            />

            <Route
              path="/alerts"
              element={<Alerts />}
            />

            <Route
              path="/predictions"
              element={<Predictions />}
            />

            <Route
              path="/root-cause"
              element={<RootCause />}
            />

            <Route
              path="/self-healing"
              element={<SelfHealing />}
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;