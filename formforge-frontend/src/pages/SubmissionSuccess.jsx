import { Link } from 'react-router-dom';
import './SubmissionSuccess.css';

function SubmissionSuccess() {
    return (
        <div className="success-page">
            <div className="success-container">
                <div className="success-icon">✓</div>
                <h1>Thank You!</h1>
                <p>Your response has been submitted successfully.</p>
                <Link to="/" className="btn btn-secondary">
                    Back to Home
                </Link>
            </div>
        </div>
    );
}

export default SubmissionSuccess;
