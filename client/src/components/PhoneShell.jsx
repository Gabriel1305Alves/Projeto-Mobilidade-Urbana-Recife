export default function PhoneShell({ children }) {
  return (
    <div className="stage">
      <div className="phone">
        <div className="phone-bar">
          <span>9:41</span>
          <span className="notch" />
          <span>LTE ████</span>
        </div>
        <div className="phone-body">{children}</div>
      </div>
    </div>
  );
}
