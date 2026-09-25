import {
  Activity,
  AlertTriangle,
  Brain,
  LayoutDashboard,
  Network,
  Server,
  Wrench,
} from "lucide-react";
import { NavLink } from "react-router-dom";

function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="logo">
        <Activity size={28} />
        <span>AIOps</span>
      </div>

      <nav>
        <div className="nav-section">
          <span className="nav-title">MONITORING</span>

          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <LayoutDashboard size={20} />
            Dashboard
          </NavLink>

          <NavLink
            to="/devices"
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <Server size={20} />
            Devices
          </NavLink>

          <NavLink
            to="/metrics"
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <Network size={20} />
            Metrics
          </NavLink>

          <NavLink
            to="/alerts"
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <AlertTriangle size={20} />
            Alerts
          </NavLink>
        </div>

        <div className="nav-section">
          <span className="nav-title">INTELLIGENCE</span>

          <NavLink
            to="/predictions"
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <Brain size={20} />
            Predictions
          </NavLink>

          <NavLink
            to="/root-cause"
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <Activity size={20} />
            Root Cause
          </NavLink>

          <NavLink
            to="/self-healing"
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <Wrench size={20} />
            Self Healing
          </NavLink>
        </div>
      </nav>

      <div className="sidebar-footer">
        <span className="status-dot"></span>
        Monitoring Active
      </div>
    </aside>
  );
}

export default Sidebar;