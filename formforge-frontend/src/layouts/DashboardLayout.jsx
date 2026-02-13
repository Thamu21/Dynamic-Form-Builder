import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './DashboardLayout.css';

function DashboardLayout() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    return (
        <div className="dashboard-layout">
            {/* Header */}
            <header className="dashboard-header">
                <div className="header-left">
                    <Link to="/" className="logo">
                        <span className="logo-icon">⬡</span>
                        <span className="logo-text gradient-text">FormForge</span>
                    </Link>
                </div>

                <nav className="header-nav">
                    <Link to="/" className="nav-link">Dashboard</Link>
                </nav>

                <div className="header-right">
                    <div className="user-menu">
                        <span className="user-name">{user?.fullName}</span>
                        <span className="user-role badge badge-info">{user?.role}</span>
                        <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
                            Logout
                        </button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="dashboard-main">
                <Outlet />
            </main>
        </div>
    );
}

export default DashboardLayout;
