export default {
  logo: (
    <span
      style={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <img
        src="logo.svg"
        style={{
          width: "1em",
          display: "inline-block",
          textAlign: "center",
        }}
      ></img>
      <span style={{ paddingLeft: "0.5em", fontSize: "1.5em" }}>
        <strong>Scala.php</strong>
      </span>
    </span>
  ),
  project: {
    link: "https://github.com/kubukoz/scala-php",
  },
  useNextSeoProps() {
    return {
      titleTemplate: "%s - Scala.php",
    };
  },
  // ... other theme options
};
